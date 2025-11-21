package com.bank.common.response;

import com.bank.shared.model.async.AsyncResponse;
import com.bank.shared.model.async.ErrorResponse;
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
