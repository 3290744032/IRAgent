package com.suiyuan.iragent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MasteryResponse {
    private Long userId;
    private List<MasteryRecordResponse> masteryRecords;
}
