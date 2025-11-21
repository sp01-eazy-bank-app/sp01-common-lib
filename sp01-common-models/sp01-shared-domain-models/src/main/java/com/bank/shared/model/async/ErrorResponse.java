package com.bank.shared.model.async;

import java.util.List;

public record ErrorResponse(String code, List<String> message) {
}
