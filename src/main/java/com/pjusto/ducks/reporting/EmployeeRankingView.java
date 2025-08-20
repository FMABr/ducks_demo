package com.pjusto.ducks.reporting;

import java.math.BigDecimal;

public interface EmployeeRankingView {
    Long getEmployeeId();
    String getEmployeeName();
    Long getSaleCount();
    BigDecimal getRevenue();
}
