package es.fhir.rest.core.model.util.transformer.helper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Optional;

import org.apache.commons.codec.digest.DigestUtils;
import org.hl7.fhir.dstu3.model.Appointment.AppointmentStatus;
import org.hl7.fhir.dstu3.model.Slot.SlotStatus;

import info.elexis.server.core.connector.elexis.jpa.model.annotated.Termin;

public class TerminHelper {

	public AppointmentStatus getStatus(Termin localObject) {
		String terminStatus = localObject.getTerminStatus();

		// TODO we need a mapping in the core, which
		// is already present for RH, for example:
		if ("eingetroffen".equalsIgnoreCase(terminStatus)) {
			return AppointmentStatus.ARRIVED;
		}

		return AppointmentStatus.BOOKED;
	}

	public SlotStatus getSlotStatus(Termin localObject) {
		// it already is an appointment, so it has to be busy
		return SlotStatus.BUSY;
	}

	public String getDescription(Termin localObject) {
		String grund = localObject.getGrund();
		if (grund == null || grund.length() < 1) {
			return localObject.getTerminTyp();
		}
		return grund;
	}

	public Optional<Object[]> getStartEndDuration(Termin localObject) {

		LocalDate day = null;
		if (localObject.getTag() != null) {
			day = localObject.getTag();
		}

		Long begin = null;
		try {
			begin = Long.parseLong(localObject.getBeginn());
		} catch (NumberFormatException nfe) {
		}

		Long duration = null;
		try {
			duration = Long.parseLong(localObject.getDauer());
		} catch (NumberFormatException nfe) {
		}

		if (day != null && begin != null && duration != null) {
			Object[] ret = new Object[3];
			LocalDateTime beginTime = day.atStartOfDay().plusMinutes(begin);
			Date startDate = Date.from(ZonedDateTime.of(beginTime, ZoneId.systemDefault()).toInstant());
			ret[0] = startDate;

			LocalDateTime endTime = beginTime.plusMinutes(duration);
			Date endDate = Date.from(ZonedDateTime.of(endTime, ZoneId.systemDefault()).toInstant());
			ret[1] = endDate;

			ret[2] = duration.intValue();
			return Optional.of(ret);
		}

		return Optional.empty();
	}

	public String getIdForBereich(String area) {
		return DigestUtils.md5Hex(area);
	}

}
