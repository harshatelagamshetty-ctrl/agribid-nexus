package com.agribid.nexus.service;

import com.agribid.nexus.dto.request.FieldRegisterRequest;
import com.agribid.nexus.dto.response.FieldResponse;
import com.agribid.nexus.security.UserPrincipal;

import java.util.List;

public interface FieldService {
    FieldResponse registerField(FieldRegisterRequest request, UserPrincipal farmer);
    List<FieldResponse> getMyFields(UserPrincipal farmer);
}
