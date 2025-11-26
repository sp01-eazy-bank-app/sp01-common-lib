package com.bank.models.shared.async;

import java.util.List;

public record ErrorResponse(String code, List<String> message) {
}
