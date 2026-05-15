#!/usr/bin/env pwsh
# release2.1 multi-tenancy cutover — runs the database half of the runbook.
#
# Phases (run in order, with the BE deploy happening between Phase 1 and Phase 3):
#
#   1. .\scripts\prod_cutover.ps1 -Action backup                   # before stopping old BE
#   2. .\scripts\prod_cutover.ps1 -Action prep                     # after stopping old BE
#   --- deploy new BE + new FE, watch logs for "Started OrderTrackerApplication" ---
#   3. .\scripts\prod_cutover.ps1 -Action verify                   # confirm migration ran clean
#   4. .\scripts\prod_cutover.ps1 -Action create-superadmin        # insert platform admin
#
# Connection params: pass on the command line, or set env vars
#   PROD_DB_HOST, PROD_DB_PORT, PROD_DB_USER, PROD_DB_NAME, PROD_DB_PASSWORD
#
# The script prompts for the password if not provided either way.

[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('backup', 'prep', 'verify', 'create-superadmin', 'preflight')]
    [string]$Action,

    [string]$DbHost     = $(if ($env:PROD_DB_HOST) { $env:PROD_DB_HOST } else { 'autorack.proxy.rlwy.net' }),
    [int]   $DbPort     = $(if ($env:PROD_DB_PORT) { [int]$env:PROD_DB_PORT } else { 16875 }),
    [string]$DbUser     = $(if ($env:PROD_DB_USER) { $env:PROD_DB_USER } else { 'root' }),
    [string]$DbName     = $(if ($env:PROD_DB_NAME) { $env:PROD_DB_NAME } else { 'railway' }),
    [string]$DbPassword = $env:PROD_DB_PASSWORD,

    [string]$BackupPath = "C:\Users\ljubi\backups\cbd_prod_$(Get-Date -Format 'yyyy-MM-dd_HH-mm-ss').sql"
)

$ErrorActionPreference = 'Stop'

# Bcrypt hash of the prod superadmin password.
# Cleartext is NOT stored here — it was shared once over chat and lives only in the operator's password manager.
$SuperadminPasswordHash = '$2b$10$l76IpiuLYNp5ah1UODINueq/B24GlOAqBpbiTOWtG91nanXkn5ObO'
$SuperadminUsername     = 'superadmin'
$SuperadminFullName     = 'Platform Superadmin'

if (-not $DbPassword) {
    $secure = Read-Host "DB password for ${DbUser}@${DbHost}:${DbPort}" -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    $DbPassword = [Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr)
}

function Invoke-Mysql {
    param(
        [Parameter(Mandatory = $true)][string]$Sql,
        [switch]$Tabular
    )
    $args = @(
        '-h', $DbHost,
        '-P', $DbPort,
        '-u', $DbUser,
        "-p$DbPassword",
        '--ssl-mode=DISABLED',
        $DbName
    )
    if ($Tabular) { $args += '-t' }
    $Sql | & mysql @args
    if ($LASTEXITCODE -ne 0) {
        throw "mysql exited with code $LASTEXITCODE"
    }
}

function Section($name) {
    Write-Host ""
    Write-Host "=== $name ===" -ForegroundColor Cyan
}

switch ($Action) {

    'preflight' {
        Section "Pre-flight checks (read-only)"
        Invoke-Mysql -Tabular -Sql @"
SELECT 'tenant exists?' AS check_name, COUNT(*) AS n FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = '$DbName' AND TABLE_NAME = 'tenant';
SELECT 'users.id AUTO_INCREMENT?' AS check_name,
       SUM(CASE WHEN EXTRA LIKE '%auto_increment%' THEN 1 ELSE 0 END) AS auto_inc
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = '$DbName' AND TABLE_NAME = 'users' AND COLUMN_NAME = 'id';
SELECT 'role.id AUTO_INCREMENT?' AS check_name,
       SUM(CASE WHEN EXTRA LIKE '%auto_increment%' THEN 1 ELSE 0 END) AS auto_inc
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = '$DbName' AND TABLE_NAME = 'role' AND COLUMN_NAME = 'id';
SELECT 'privilege.id AUTO_INCREMENT?' AS check_name,
       SUM(CASE WHEN EXTRA LIKE '%auto_increment%' THEN 1 ELSE 0 END) AS auto_inc
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = '$DbName' AND TABLE_NAME = 'privilege' AND COLUMN_NAME = 'id';
SELECT 'orders count' AS check_name, COUNT(*) AS n FROM order_record;
SELECT 'users count'  AS check_name, COUNT(*) AS n FROM users;
SELECT 'banners count' AS check_name, COUNT(*) AS n FROM banner;
SELECT MAX(id) AS users_max_id FROM users;
SELECT MAX(id) AS role_max_id FROM role;
SELECT MAX(id) AS privilege_max_id FROM privilege;
SELECT MAX(CHAR_LENGTH(text)) AS longest_banner_text FROM banner;
"@
    }

    'backup' {
        Section "Backing up prod to $BackupPath"
        $parent = Split-Path $BackupPath -Parent
        if ($parent -and -not (Test-Path $parent)) {
            New-Item -ItemType Directory -Path $parent -Force | Out-Null
        }
        & mysqldump `
            -h $DbHost `
            -P $DbPort `
            -u $DbUser `
            "-p$DbPassword" `
            --ssl-mode=DISABLED `
            --single-transaction `
            --routines `
            --triggers `
            $DbName | Out-File -FilePath $BackupPath -Encoding utf8
        if ($LASTEXITCODE -ne 0) { throw "mysqldump exited with code $LASTEXITCODE" }
        $size = (Get-Item $BackupPath).Length
        Write-Host "Backup OK. Size: $([math]::Round($size/1MB, 2)) MB → $BackupPath" -ForegroundColor Green
        if ($size -lt 100000) {
            Write-Warning "Backup is suspiciously small (<100 KB). Inspect before proceeding."
        }
    }

    'prep' {
        Section "Reading current MAX(id) values (informational)"
        Invoke-Mysql -Tabular -Sql @"
SELECT MAX(id) AS users_max_id FROM users;
SELECT MAX(id) AS role_max_id  FROM role;
SELECT MAX(id) AS privilege_max_id FROM privilege;
"@

        Section "Applying AUTO_INCREMENT fixes on users.id, role.id, privilege.id"
        # users.id is INT not BIGINT — match the existing column type.
        # AUTO_INCREMENT starting values are deliberately generous; MySQL clamps to MAX(id)+1 anyway.
        Invoke-Mysql -Sql @'
SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE users     MODIFY id INT    NOT NULL AUTO_INCREMENT;
ALTER TABLE users     AUTO_INCREMENT = 1000;
ALTER TABLE role      MODIFY id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE role      AUTO_INCREMENT = 100;
ALTER TABLE privilege MODIFY id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE privilege AUTO_INCREMENT = 100;
SET FOREIGN_KEY_CHECKS = 1;
'@

        Section "Verifying"
        Invoke-Mysql -Tabular -Sql @"
SHOW COLUMNS FROM users     WHERE Field = 'id';
SHOW COLUMNS FROM role      WHERE Field = 'id';
SHOW COLUMNS FROM privilege WHERE Field = 'id';
"@
        Write-Host "All three columns should now show 'auto_increment' in Extra." -ForegroundColor Green
        Write-Host "Next: deploy the new BE + FE. Watch logs for 'Started OrderTrackerApplication'." -ForegroundColor Yellow
    }

    'verify' {
        Section "Migration verification (run AFTER the new BE has started)"
        Invoke-Mysql -Tabular -Sql @"
SELECT 'orders_missing_tenant'  AS check_name, COUNT(*) AS missing FROM order_record WHERE tenant_id IS NULL
UNION ALL SELECT 'users_missing_tenant',   COUNT(*) FROM users  WHERE superadmin = false AND tenant_id IS NULL
UNION ALL SELECT 'banners_missing_tenant', COUNT(*) FROM banner WHERE tenant_id IS NULL;

SELECT id, name, slug, active+0 AS active FROM tenant;

SELECT id, name FROM role ORDER BY id;

SELECT COUNT(*) AS orders_with_tenant_1 FROM order_record WHERE tenant_id = 1;
SELECT COUNT(*) AS users_with_tenant_1  FROM users  WHERE tenant_id = 1;
SELECT COUNT(*) AS banners_with_tenant_1 FROM banner WHERE tenant_id = 1;

SELECT MAX(CHAR_LENGTH(text)) AS longest_banner_text, COUNT(*) AS banners FROM banner;
"@
        Write-Host @"

Expected results:
  - All three 'missing' rows = 0
  - tenant: id=1, name=CBD, slug=cbd, active=1
  - role table: company_admin, manufacturer, manager (no 'admin')
  - tenant_1 counts match what you measured pre-migration
"@ -ForegroundColor Green
    }

    'create-superadmin' {
        Section "Checking whether 'superadmin' user already exists"
        Invoke-Mysql -Tabular -Sql @"
SELECT COUNT(*) AS existing_count FROM users WHERE username = '$SuperadminUsername';
"@
        Write-Host "If existing_count > 0, the INSERT below will fail on the UNIQUE constraint. Inspect before re-running." -ForegroundColor Yellow

        Section "Inserting platform superadmin"
        $sql = @"
INSERT INTO users (username, full_name, password, superadmin, tenant_id, created_at, updated_at)
VALUES ('$SuperadminUsername', '$SuperadminFullName', '$SuperadminPasswordHash', true, NULL, NOW(), NOW());

SELECT ROW_COUNT() AS rows_inserted, LAST_INSERT_ID() AS new_id;

SELECT id, username, full_name, superadmin+0 AS superadmin, tenant_id, LEFT(password, 7) AS pw_prefix
FROM users WHERE username = '$SuperadminUsername';
"@
        Invoke-Mysql -Tabular -Sql $sql
        Write-Host "" -ForegroundColor Green
        Write-Host "Superadmin user inserted. Log in with username '$SuperadminUsername' and the password from the password manager." -ForegroundColor Green
    }
}
