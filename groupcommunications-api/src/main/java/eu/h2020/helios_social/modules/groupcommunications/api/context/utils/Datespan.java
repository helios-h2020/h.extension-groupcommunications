package eu.h2020.helios_social.modules.groupcommunications.api.context.utils;

import java.time.LocalDate;

public class Datespan {

    private LocalDate startDate;
    private LocalDate endDate;

    public Datespan(LocalDate startDate, LocalDate endDate) {
        if (startDate.isBefore(endDate)) {
            this.startDate = startDate;
            this.endDate = endDate;
        } else {
            this.startDate = endDate;
            this.endDate = startDate;
        }
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public boolean contains(LocalDate date) {
        return date.isAfter(startDate) && date.isBefore(endDate);
    }
}
