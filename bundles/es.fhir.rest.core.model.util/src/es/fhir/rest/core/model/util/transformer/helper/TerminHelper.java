package es.fhir.rest.core.model.util.transformer.helper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Optional;

import org.hl7.fhir.dstu3.model.Appointment.AppointmentStatus;
import org.hl7.fhir.dstu3.model.Slot.SlotStatus;

import info.elexis.server.core.connector.elexis.jpa.model.annotated.Termin;

public class TerminHelper {

	public AppointmentStatus getStatus(Termin localObject) {
		String terminStatus = localObject.getTerminStatus();

		// TODO we need a dynamic mapping in the core, like it
		// is already present for RH, for example:
		switch (terminStatus) {
		case "eingetroffen":
			return AppointmentStatus.ARRIVED;
		case "erledigt":
			return AppointmentStatus.FULFILLED;
		case "abgesagt":
			return AppointmentStatus.CANCELLED;
		case "nicht erschienen":
			return AppointmentStatus.NOSHOW;
		default:
			return AppointmentStatus.BOOKED;
		}
	}

	public SlotStatus getSlotStatus(Termin localObject) {
		String terminTyp = localObject.getTerminTyp();

		// TODO we need a dynamic mapping in the core, like it
		// is already present for RH, for example:
		switch (terminTyp) {
		case "frei":
			return SlotStatus.FREE;
		case "gesperrt":
			return SlotStatus.BUSYUNAVAILABLE;
		default:
			return SlotStatus.BUSY;
		}
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

}
