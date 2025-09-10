package cbd.order_tracker.service.impl;

import cbd.order_tracker.model.User;
import cbd.order_tracker.model.company.Company;
import cbd.order_tracker.model.dto.CompanyDto;
import cbd.order_tracker.model.dto.response.CompanyOverviewResDto;
import cbd.order_tracker.repository.CompanyRepository;
import cbd.order_tracker.repository.UserRepository;
import cbd.order_tracker.service.inter.CompanyService;
import cbd.order_tracker.util.CompanyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    @Override
    public List<CompanyOverviewResDto> getAll() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println(auth.getAuthorities());
        return companyRepository.findAll()
                .stream()
                .map(CompanyMapper::toOverviewDto)
                .toList();
    }

    @Override
    public CompanyDto get(Long id) {
        var company = companyRepository.findById(id).orElseThrow(() -> new RuntimeException("Company not found"));

        return new CompanyDto(company);
    }

    @Override
    public List<CompanyOverviewResDto> getCompanies(List<Long> ids) {
        return companyRepository.findAllById(ids)
                .stream()
                .map(CompanyMapper::toOverviewDto)
                .toList();
    }

    @Override
    public CompanyDto create(CompanyDto companyDto) {
        Company companyToCreate = new Company(companyDto);
        var createdCompany = companyRepository.save(companyToCreate);
        return new CompanyDto(createdCompany);
    }

    //    Todo: check how this should work
    @Transactional
    @Override
    public CompanyDto updateEmployees(Long companyId, List<Integer> newEmployeeIds) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));
        List<User> newUsers = userRepository.findAllById(newEmployeeIds);

        if (newUsers.size() != newEmployeeIds.size()) {
            throw new RuntimeException("Some users not found");
        }

        // 3. Remove current employees associations (both sides)
        for (User user : company.getEmployees()) {
            user.getCompanies().remove(company);
        }
        company.getEmployees().clear();

        // 4. Add new employees (both sides)
        for (User user : newUsers) {
            company.getEmployees().add(user);
            user.getCompanies().add(company);
        }

        // 5. Save company - cascade persists join table changes
        return new CompanyDto(companyRepository.save(company));
    }

    @Override
    public CompanyDto updateInfo(CompanyDto companyDto) {
        Company company = companyRepository.findById(companyDto.getId())
                .orElseThrow(() -> new RuntimeException("Company not found"));

        company.setName(companyDto.getName());
        company.setCurrency(companyDto.getCurrency());
        company.setVat(companyDto.getVat());
        company.setColors(companyDto.getColors());
        company.setWebsiteUrl(companyDto.getWebsiteUrl());

        return new CompanyDto(companyRepository.save(company));
    }
}
