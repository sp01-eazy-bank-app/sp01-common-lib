package com.bank.models.kyc.response;

import com.bank.models.shared.async.AsyncResponse;
import com.bank.models.shared.async.ErrorResponse;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class KycVerificationAsyncResponse extends AsyncResponse<KycVerificationReply> {

    public KycVerificationAsyncResponse(KycVerificationReply data) {
        super(data, null);
    }

    public KycVerificationAsyncResponse(ErrorResponse error) {
        super(null, error);
    }
}
