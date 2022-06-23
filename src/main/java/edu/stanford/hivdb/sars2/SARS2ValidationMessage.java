package edu.stanford.hivdb.sars2;

import edu.stanford.hivdb.utilities.ValidationLevel;
import edu.stanford.hivdb.utilities.ValidationMessage;

public enum SARS2ValidationMessage implements ValidationMessage {
	
	NoGeneFound(
		ValidationLevel.CRITICAL,
		"There were no %s genes found, refuse to process."
	),
	FASTAGapTooLong(
		ValidationLevel.CRITICAL,
		"This sequence has critical potentially correctable errors. It has a large sequence gap, " +
		"defined as an insertion or deletion of >60 bps. One possible cause of this error is that " +
		"the input sequence was concatenated from multiple partial sequences. Adding 'N's in place " +
		"of the missing sequence will allow the sequence to be processed."
	),
	FASTASequenceTrimmed(
		ValidationLevel.WARNING,
		"The %s sequence had %d amino acids trimmed from its %s-end due to poor quality."
	),
	FASTAInvalidNAsRemoved(
		ValidationLevel.NOTE,
		"Non-NA character(s) %s were found and removed from the sequence."
	),
	MultiplePositionsMissingWithMultipleDRPs(
		ValidationLevel.WARNING,
		"%d positions were not sequenced or aligned: %s. Of them, %d are at drug-resistance positions: %s."
	),
	MultiplePositionsMissingWithSingleDRP(
		ValidationLevel.WARNING,
		"%d positions were not sequenced or aligned: %s. Of them, one is at drug-resistance position: %s."
	),
	SingleDRPMissing(
		ValidationLevel.NOTE,
		"One drug-resistance position " +
		"was not sequenced or aligned: %s."
	),
	MultiplePositionsMissingWithoutDRP(
		ValidationLevel.WARNING,
		"%d positions were not sequenced or aligned: %s. However, none is at drug-resistance position."
	),
	SinglePositionMissingWithoutDRP(
		ValidationLevel.NOTE,
		"One non-drug-resistance position was not sequenced or aligned: %s."
	),
	NGSMinReadDepthTooLow(
		ValidationLevel.WARNING,
		"%d (%.1f%%) %s position%s with below-minimum read depth (<%d) %s been trimmed: %s."
	),
	MultipleStopCodons(
		ValidationLevel.WARNING,
		"There are %d stop codons in %s: %s."
	),
	SingleStopCodon(
		ValidationLevel.WARNING,
		"There is one stop codon in %s: %s."
	),
	MultipleUnusualMutations(
		ValidationLevel.WARNING,
		"There are %d unusual mutations in %s: %s."
	),
	MultipleUnusualIndelsAndFrameshifts(
		ValidationLevel.SEVERE_WARNING,
		"The %s gene has %d unusual indels and/or frameshifts. " +
		"The indels include %s. The frameshifts include %s."
	),
	MultipleFrameShifts(
		ValidationLevel.SEVERE_WARNING,
		"The %s gene has %d frameshifts: %s."
	),
	MultipleUnusualIndels(
		ValidationLevel.SEVERE_WARNING,
		"The %s gene has %d unusual indels: %s."
	),
	SingleFrameshift(
		ValidationLevel.WARNING,
		"The %s gene has a frameshift: %s."
	),
	SingleUnusualIndel(
		ValidationLevel.WARNING,
		"The %s gene has an unusual indel: %s."
	),
	FASTAReverseComplement(
		ValidationLevel.WARNING,
		"This report was derived from the reverse complement of input sequence."
	),
	NGSMixtureRateTooHigh(
		ValidationLevel.SEVERE_WARNING,
		"The nuecleotide mixture rate of this sequence at current threshold set " +
		"is too high (%.1f%%) and may result in sequence artifacts. Consider " +
		"choosing a higher mutation detection threshold or a lower nucleotide " +
		"mixture threshold."
	);
	
	public final ValidationLevel level;
	public final String template;
	
	SARS2ValidationMessage(ValidationLevel level, String template) {
		this.level = level;
		this.template = template;
	}
	
	@Override
	public final ValidationLevel getLevel() {
		return level;
	}
	
	@Override
	public final String getTemplate() {
		return template;
	}
}