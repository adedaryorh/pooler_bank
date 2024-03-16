package com.poolerapp.pooler_bank.service;

import com.poolerapp.pooler_bank.config.JwtTokenGenerator;
import com.poolerapp.pooler_bank.dto.*;
import com.poolerapp.pooler_bank.enums.CustomerAccountStatus;
import com.poolerapp.pooler_bank.enums.CustomerAccountType;
import com.poolerapp.pooler_bank.enums.Role;
import com.poolerapp.pooler_bank.model.AccountCustomer;
import com.poolerapp.pooler_bank.repository.AccountCustomerRepository;
import com.poolerapp.pooler_bank.utils.AccountUtils;
import org.springframework.transaction.annotation.Transactional;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataAccessException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.logging.Logger;



@Slf4j
@Service
@Builder
public class AccountCustomerServiceImpl implements AccountCustomerService{

    private static final Logger logger = Logger.getLogger(TransactionServiceImpl.class.getName());

    @Autowired
    AccountCustomerRepository accountCustomerRepository;

    @Autowired
    TransactionService transactionService;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JwtTokenGenerator jwtTokenGenerator;


    @Override
    public BankResponse creatAnAnccount(AccountCustomerRequest accountCustomerRequest) {
        //TODO: Create a new customer account and persist the record
        //TODO: check if customer record exit
        //TODO: bal enquiry, Name enquiry, CR, DR and transfer
        if (accountCustomerRepository.existsByAccountNumber(accountCustomerRequest.getAccountNumber())){
            BankResponse response = BankResponse.builder()
                    .responseCode(AccountUtils.ACCOUNT_EXISTS_CODE)
                    .responseMessage(AccountUtils.ACCOUNT_EXISTS_MSG)
                    .accountCustomerInfomation(null)
                    .build();
            return response;
        }
        if (accountCustomerRepository.existsByEmail(accountCustomerRequest.getEmail())){
            BankResponse response = BankResponse.builder()
                    .responseCode(AccountUtils.ACCOUNT_EXISTS_CODE)
                    .responseMessage(AccountUtils.ACCOUNT_EXISTS_MSG)
                    .accountCustomerInfomation(null)
                    .build();
            return response;
        }
        AccountCustomer newAccountCustomer = AccountCustomer.builder()
                .firstName(accountCustomerRequest.getFirstName())
                .lastName(accountCustomerRequest.getLastName())
                .email(accountCustomerRequest.getEmail())
                .dateOfBirth(accountCustomerRequest.getDateOfBirth())
                .address(accountCustomerRequest.getAddress())
                .gender(accountCustomerRequest.getGender())
                .stateOfOrigin(accountCustomerRequest.getStateOfOrigin())
                .accountNumber(AccountUtils.generateAccountnumber())
                .accountBalance(BigDecimal.ZERO)
                .phonenumber(accountCustomerRequest.getPhonenumber())
                .ninNumber(accountCustomerRequest.getNinNumber())
                .bvnNumber(accountCustomerRequest.getBvnNumber())
                .password(passwordEncoder.encode(accountCustomerRequest.getPassword()))
                .customerAccountType(accountCustomerRequest.getCustomerAccountType())
                .customerAccountStatus(accountCustomerRequest.getCustomerAccountStatus())
                .role(Role.valueOf("ROLE_ADMIN"))
                .build();
        AccountCustomer savedAccountCustomer = accountCustomerRepository.save(newAccountCustomer);
         BankResponse response = BankResponse.builder()
                 .responseCode(AccountUtils.ACCOUNT_CREATION_SUCCESS_CODE)
                 .responseMessage(AccountUtils.ACCOUNT_CREATION_SUCCESS_MSG)
                 .accountCustomerInfomation(AccountCustomerInfomation.builder()
                         .accountBalance(savedAccountCustomer.getAccountBalance())
                         .accountNumber(savedAccountCustomer.getBvnNumber())
                         .accountName(
                                 savedAccountCustomer.getFirstName() +""+
                                         savedAccountCustomer.getLastName()+""+
                                         savedAccountCustomer.getEmail())
                         .build())
                .build();
         return response;
    }
    public BankResponse login(LoginDto loginDto){
        Authentication auth = null;
        auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getPassword()));
        return BankResponse.builder()
                .responseCode("Login Success")
                .responseMessage(jwtTokenGenerator.generateToken(auth))
                .build();
    }
    @Override
    public BankResponse balanceEnquiry(EnquiryRequest enquiryRequest) {
        //TODO: check if the account number exist in the DB

        boolean isAccountCustomerExist = accountCustomerRepository.existsByAccountNumber(enquiryRequest.getAccountNumber());
        if (!isAccountCustomerExist){
            return BankResponse.builder()
                    .responseCode(AccountUtils.ACCOUNT_NOT_EXISTS_CODE)
                    .responseMessage(AccountUtils.ACCOUNT_NOT_EXISTS_MSG)
                    .accountCustomerInfomation(null)
                    .build();
        }

        //if customer exists
        AccountCustomer availableAccountCustomer = accountCustomerRepository.findByAccountNumber(enquiryRequest.getAccountNumber());
        BankResponse response = BankResponse.builder()
                .responseCode(AccountUtils.ACCOUNT_FOUND_CODE)
                .responseMessage(AccountUtils.ACCOUNT_FOUND_MSG)
                .accountCustomerInfomation(AccountCustomerInfomation.builder()
                        .accountBalance(availableAccountCustomer.getAccountBalance())
                        .accountNumber(availableAccountCustomer.getAccountNumber())
                        .accountName(availableAccountCustomer.getFirstName() + "" + availableAccountCustomer.getLastName() + "" + availableAccountCustomer.getEmail())
                        .build())
                .build();
        return response;
    }
    @Override
    public String nameEnquiry(EnquiryRequest enquiryRequest) {
        boolean isAccountCustomerExist = accountCustomerRepository.existsByAccountNumber(enquiryRequest.getAccountNumber());
        if (!isAccountCustomerExist){
            return AccountUtils.ACCOUNT_NOT_EXISTS_MSG;
        }else {
            AccountCustomer availableAccountCustomer = accountCustomerRepository.findByAccountNumber(enquiryRequest.getAccountNumber());
            return availableAccountCustomer.getFirstName()+""+
                    availableAccountCustomer.getLastName()+""+
                    availableAccountCustomer.getEmail();

        }
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BankResponse creditingAnAccount(CreditDebitRequest creditDebitRequest) {
        //confirm acc exist
        boolean isAccountCustomerExist = accountCustomerRepository.existsByAccountNumber(creditDebitRequest.getAccountNumber());
        if (!isAccountCustomerExist){
            return BankResponse.builder()
                    .responseCode(AccountUtils.ACCOUNT_NOT_EXISTS_CODE)
                    .responseMessage(AccountUtils.ACCOUNT_NOT_EXISTS_MSG)
                    .accountCustomerInfomation(null)
                    .build();
        }
        AccountCustomer customerToCredit = accountCustomerRepository.findByAccountNumber(creditDebitRequest.getAccountNumber());
        customerToCredit.setAccountBalance(customerToCredit.getAccountBalance().add(creditDebitRequest.getAmount()));
        accountCustomerRepository.save(customerToCredit);

        TransactionDTO transactionDTO = TransactionDTO.builder()
                .accountNumber(customerToCredit.getAccountNumber())
                .transactionType("CREDIT")
                .amount(creditDebitRequest.getAmount())
                .build();
        transactionService.saveTransaction(transactionDTO);

        return BankResponse.builder()
                .responseCode(AccountUtils.ACCOUNT_CREDITED_SUCCESS_CODE)
                .responseMessage(AccountUtils.ACCOUNT_CREDITED_SUCCESS_MSG)
                .accountCustomerInfomation(AccountCustomerInfomation.builder()
                        .accountName(customerToCredit.getFirstName() + " " + customerToCredit.getLastName() + " " + customerToCredit.getEmail())
                        .accountBalance(customerToCredit.getAccountBalance())
                        .accountNumber(creditDebitRequest.getAccountNumber())
                        .build())
                .build();
    }
    public BankResponse debitingAnAccount(CreditDebitRequest creditDebitRequest) {
        try {
            return debitingAnAccountTransactional(creditDebitRequest);
        } catch (DataAccessException ex) {
            return BankResponse.builder()
                    .responseCode(AccountUtils.DATABASE_ERROR_CODE)
                    .responseMessage(AccountUtils.DATABASE_ERROR_MSG)
                    .accountCustomerInfomation(null)
                    .build();
        }
    }
    @Transactional(rollbackFor = Exception.class)
    public BankResponse debitingAnAccountTransactional(CreditDebitRequest creditDebitRequest) {
        //TODO: check if the account number exist in the DB
        //TODO: check if the amount to dr is not > current bal
        boolean isAccountCustomerExist = accountCustomerRepository.existsByAccountNumber(creditDebitRequest.getAccountNumber());
        if (!isAccountCustomerExist){
            return BankResponse.builder()
                    .responseCode(AccountUtils.ACCOUNT_NOT_EXISTS_CODE)
                    .responseMessage(AccountUtils.ACCOUNT_NOT_EXISTS_MSG)
                    .accountCustomerInfomation(null)
                    .build();
        }
        AccountCustomer customerToDebit = accountCustomerRepository.findByAccountNumber(creditDebitRequest.getAccountNumber());
        BigInteger availableBalance = customerToDebit.getAccountBalance().toBigInteger();
        BigInteger debitAmount = creditDebitRequest.getAmount().toBigInteger();

        if (availableBalance.compareTo(debitAmount) < 0){
            return BankResponse.builder()
                    .responseCode(AccountUtils.ACCOUNT_INSUFFICIENT_ERROR_CODE)
                    .responseMessage(AccountUtils.ACCOUNT_INSUFFICIENT_ERROR_MSG)
                    .accountCustomerInfomation(null)
                    .build();
        } else {
            customerToDebit.setAccountBalance(customerToDebit.getAccountBalance().subtract(creditDebitRequest.getAmount()));
            accountCustomerRepository.save(customerToDebit);

            TransactionDTO transactionDTO = TransactionDTO.builder()
                    .accountNumber(customerToDebit.getAccountNumber())
                    .transactionType("DEBIT")
                    .amount(creditDebitRequest.getAmount())
                    .build();

            transactionService.saveTransaction(transactionDTO);
            return BankResponse.builder()
                    .responseCode(AccountUtils.ACCOUNT_DEBITED_SUCCESS_CODE)
                    .responseMessage(AccountUtils.ACCOUNT_DEBITED_SUCCESS_MSG)
                    .accountCustomerInfomation(AccountCustomerInfomation.builder()
                            .accountName(customerToDebit.getFirstName() + " " + customerToDebit.getLastName() + " " + customerToDebit.getEmail())
                            .accountNumber(customerToDebit.getAccountNumber())
                            .accountBalance(customerToDebit.getAccountBalance())
                            .build())
                    .build();
        }
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BankResponse transfer(TransferRequest transferRequest) {
        //TODO: get the dr acct and validate as necessary
        //TODO: confirm the dr accct bal is sufficient
        //TODO: dr leg and get the cr acct
        //TODO: cr the destination acct
        boolean isDestinationAccountExist = accountCustomerRepository.existsByAccountNumber(transferRequest.getDestinationAccountNumber());
        if (!isDestinationAccountExist){
            return BankResponse.builder()
                    .responseCode(AccountUtils.ACCOUNT_NOT_EXISTS_CODE)
                    .responseMessage(AccountUtils.ACCOUNT_NOT_EXISTS_MSG)
                    .accountCustomerInfomation(null)
                    .build();
        }
        AccountCustomer sourceCustomerAccount = accountCustomerRepository.findByAccountNumber(transferRequest.getSourceAccountNumber());
        if (transferRequest.getAmount().compareTo(sourceCustomerAccount.getAccountBalance()) > 0){
            return BankResponse.builder()
                    .responseCode(AccountUtils.ACCOUNT_INSUFFICIENT_ERROR_CODE)
                    .responseMessage(AccountUtils.ACCOUNT_INSUFFICIENT_ERROR_MSG)
                    .accountCustomerInfomation(null)
                    .build();
        }
        sourceCustomerAccount.setAccountBalance(sourceCustomerAccount.getAccountBalance().subtract(transferRequest.getAmount()));
        accountCustomerRepository.save(sourceCustomerAccount);
        AccountCustomer destinationAccountUser = accountCustomerRepository.findByAccountNumber(transferRequest.getDestinationAccountNumber());
        destinationAccountUser.setAccountBalance(destinationAccountUser.getAccountBalance().add(transferRequest.getAmount()));
        accountCustomerRepository.save(destinationAccountUser);

        TransactionDTO transactionDTO = TransactionDTO.builder()
                .accountNumber(destinationAccountUser.getAccountNumber())
                .transactionType("CREDIT")
                .amount(transferRequest.getAmount())
                .build();

        transactionService.saveTransaction(transactionDTO);

        return BankResponse.builder()
                .responseCode(AccountUtils.ACCOUNT_TRANSFER_SUCCESS_CODE)
                .responseMessage(AccountUtils.ACCOUNT_TRANSFER_SUCCESS_MSG)
                .accountCustomerInfomation(AccountCustomerInfomation.builder()
                        .accountNumber(transferRequest.getDestinationAccountNumber() +" "+ "Credited with "+ " "+transferRequest.getAmount()+" "+ "value")
                        .build())
                .build();
    }
    @Override
    public AccountTypeResponse updateAccountType(AccountTypeRequest accountTypeRequest) {
        boolean isAccountExist = accountCustomerRepository.existsByAccountNumber(accountTypeRequest.getAccountNumber());
        if (!isAccountExist) {
            return AccountTypeResponse.builder()
                    .responseCode(AccountUtils.ACCOUNT_NOT_EXISTS_CODE)
                    .responseMessage(AccountUtils.ACCOUNT_NOT_EXISTS_MSG)
                    .build();
        }
        AccountCustomer accountCustomer = accountCustomerRepository.findByAccountNumber(accountTypeRequest.getAccountNumber());
        CustomerAccountType newAccountType = CustomerAccountType.valueOf(accountTypeRequest.getAccountType());
        accountCustomer.setCustomerAccountType(newAccountType);
        accountCustomerRepository.save(accountCustomer);
        return AccountTypeResponse.builder()
                .responseCode(AccountUtils.ACCOUNT_UPDATED_SUCCESS_CODE)
                .responseMessage(AccountUtils.ACCOUNT_UPDATED_SUCCESS_MSG)
                .build();
    }
    @Override
    public AccountStatusResponse updateAccountStatus(AccountStatusRequest accountStatusRequest) {
        boolean isAccountExist = accountCustomerRepository.existsByAccountNumber(accountStatusRequest.getAccountNumber());
        if (!isAccountExist) {
            return AccountStatusResponse.builder()
                    .responseCode(AccountUtils.ACCOUNT_NOT_EXISTS_CODE)
                    .responseMessage(AccountUtils.ACCOUNT_NOT_EXISTS_MSG)
                    .build();
        }
        AccountCustomer accountCustomer = accountCustomerRepository.findByAccountNumber(accountStatusRequest.getAccountNumber());
        CustomerAccountStatus newAccountStatus = CustomerAccountStatus.valueOf(accountStatusRequest.getAccountStatus());
        accountCustomer.setCustomerAccountStatus(newAccountStatus);
        accountCustomerRepository.save(accountCustomer);
        return AccountStatusResponse.builder()
                .responseCode(AccountUtils.ACCOUNT_UPDATED_SUCCESS_CODE)
                .responseMessage(AccountUtils.ACCOUNT_UPDATED_SUCCESS_MSG)
                .build();
    }
    @Override
    public AccountCustomer getAccountCustomerByAccountNumber(String accountNumber) {
        return accountCustomerRepository.findByAccountNumber(accountNumber);
    }
    @Override
    public void deleteAccountCustomerByAccountNumber(String accountNumber) {
        log.info("YOU ARE ABOUT TO CLOSED THIS ACCOUT WITH ACCTNUMBER "+ accountNumber);
        accountCustomerRepository.deleteAccountCustomerByAccountNumber(accountNumber);
    }
}

/*
  public void processAccountCustomerDateOfBirthRequest(AccountCustomerRequest accountCustomerRequest) {
        // Parse custom date format into LocalDate
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        LocalDate dateOfBirth = LocalDate.parse(accountCustomerRequest.getDateOfBirth(), formatter);
    }
 */
