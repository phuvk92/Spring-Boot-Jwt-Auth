package com.example.svgmanager.service;

import com.example.svgmanager.dto.request.CreateDealerRequest;
import com.example.svgmanager.dto.request.UpdateDealerRequest;
import com.example.svgmanager.dto.response.DealerResponse;
import com.example.svgmanager.dto.response.DealerStatsResponse;
import com.example.svgmanager.dto.response.PageResponse;

import java.util.List;

public interface DealerService {

    PageResponse<DealerResponse> getDealers(String search, String status, String region, int page, int size, String sortBy, String sortDir);

    List<DealerResponse> getAllDealers();

    DealerResponse getDealerById(Long id);

    DealerResponse createDealer(CreateDealerRequest request);

    DealerResponse updateDealer(Long id, UpdateDealerRequest request);

    void deleteDealer(Long id);

    DealerStatsResponse getDealerStats();

    DealerResponse updateStatus(Long id, String status);

    String generateUniqueCode();
}
