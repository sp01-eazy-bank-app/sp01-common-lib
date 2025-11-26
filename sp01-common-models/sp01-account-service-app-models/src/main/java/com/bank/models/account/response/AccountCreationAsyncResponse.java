package com.bank.models.account.response;

import com.bank.models.shared.async.AsyncResponse;
import com.bank.models.shared.async.ErrorResponse;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class AccountCreationAsyncResponse extends AsyncResponse<AccountReply> {

    public AccountCreationAsyncResponse(AccountReply reply) {
        super(reply, null);
    }

    public AccountCreationAsyncResponse(ErrorResponse error) {
        super(null, error);
    }
}
