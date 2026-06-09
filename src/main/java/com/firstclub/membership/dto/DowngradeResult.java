package com.firstclub.membership.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DowngradeResult {
    private Long membershipId;
    private Long userId;
    private Long previousTierId;
    private Long newTierId;

    @Override
    public String toString() {
        return "DowngradeResult{membershipId=" + membershipId + ", userId=" + userId
                + ", previousTierId=" + previousTierId + ", newTierId=" + newTierId + "}";
    }
}
