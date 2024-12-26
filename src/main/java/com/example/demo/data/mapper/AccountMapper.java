package com.example.demo.data.mapper;

import com.example.demo.data.dto.account.AccountCreateRequest;
import com.example.demo.data.dto.account.AccountInfo;
import com.example.demo.data.dto.account.Profile;
import com.example.demo.data.model.Account;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    AccountInfo toInfo(Account account);

    Profile toProfile(Account account);

    Account fromCreateRequest(AccountCreateRequest request);
}
