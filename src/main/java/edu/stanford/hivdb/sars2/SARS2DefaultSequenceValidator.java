/*

    Copyright (C) 2022 Stanford HIVDB team

    Sierra is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Sierra is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package edu.stanford.hivdb.sars2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

import edu.stanford.hivdb.viruses.Gene;
import edu.stanford.hivdb.viruses.Strain;
import edu.stanford.hivdb.mutations.Mutation;
import edu.stanford.hivdb.mutations.FrameShift;
import edu.stanford.hivdb.mutations.GenePosition;
import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sequences.AlignedGeneSeq;
import edu.stanford.hivdb.sequences.AlignedSequence;
import edu.stanford.hivdb.sequences.SequenceValidator;
import edu.stanford.hivdb.sequences.GeneRegions;
import edu.stanford.hivdb.utilities.Json;
import edu.stanford.hivdb.utilities.MyStringUtils;
import edu.stanford.hivdb.utilities.ValidationLevel;
import edu.stanford.hivdb.utilities.ValidationResult;

public class SARS2DefaultSequenceValidator implements SequenceValidator<SARS2> {

	protected SARS2DefaultSequenceValidator() {}

	@Override
	public List<ValidationResult> validate(AlignedSequence<SARS2> alignedSequence, Collection<String> includeGenes) {
		List<ValidationResult> results = new ArrayList<>();
		results.addAll(validateNotEmpty(alignedSequence, includeGenes));
		if (results.size() > 0) {
			return results;
		}
		results.addAll(validateReverseComplement(alignedSequence));
		results.addAll(validateNoMissingPositions(alignedSequence, includeGenes));
		results.addAll(validateLongGap(alignedSequence, includeGenes));
		results.addAll(validateNAs(alignedSequence));
		results.addAll(validateGaps(alignedSequence, includeGenes));
		results.addAll(validateNoStopCodons(alignedSequence, includeGenes));
		results.addAll(validateNoTooManyUnusualMutations(alignedSequence, includeGenes));
		return results;
	}

	protected static List<ValidationResult> validateNotEmpty(
		AlignedSequence<?> alignedSequence,
		Collection<String> includeGenes
	) {
		SARS2 virusIns = SARS2.getInstance();
		boolean isNotEmpty = alignedSequence.getAvailableGenes().stream()
			.anyMatch(gene -> includeGenes.contains(gene.getAbstractGene()));
		if (!isNotEmpty) {
			List<String> includeGeneDisplays = includeGenes.stream()
				.map(geneName -> virusIns.getGeneDisplay(geneName))
				.collect(Collectors.toList());
			return Lists.newArrayList(
				SARS2ValidationMessage.NoGeneFound.format(
					MyStringUtils.andListFormat(includeGeneDisplays)
				)
			);
		}
		return Collections.emptyList();
	}

	protected static List<ValidationResult> validateReverseComplement(AlignedSequence<?> alignedSequence) {
		if (alignedSequence.isReverseComplement()) {
			return Lists.newArrayList(SARS2ValidationMessage.FASTAReverseComplement.format());
		}
		return Collections.emptyList();
	}

	protected static List<ValidationResult> validateNoMissingPositions(
		final Set<GenePosition<SARS2>> needGenePositions,
		final Set<GenePosition<SARS2>> needDRGenePositions,
		final Set<GenePosition<SARS2>> availableGenePositions
	) {
		List<ValidationResult> results = new ArrayList<>();

		List<GenePosition<SARS2>> missingPositions = needGenePositions.stream()
				.filter(gp -> !availableGenePositions.contains(gp))
				.collect(Collectors.toList());
		long numMissingPositions = missingPositions.size();

		List<GenePosition<SARS2>> missingDRPs = needDRGenePositions.stream()
				.filter(gp -> !availableGenePositions.contains(gp))
				.collect(Collectors.toList());
		long numMissingDRPs = missingDRPs.size();
		
		String textMissingPositions = StringUtils.join(
			GeneRegions.newListOfGeneRegions(missingPositions),
			"; "
		);

		String textMissingDRPs = StringUtils.join(
			GeneRegions.newListOfGeneRegions(missingDRPs),
			"; "
		);
		
		if (numMissingDRPs > 1) {
			results.add(SARS2ValidationMessage.MultiplePositionsMissingWithMultipleDRPs.formatWithLevel(
				numMissingDRPs > 5 ? ValidationLevel.SEVERE_WARNING : ValidationLevel.WARNING,
				numMissingPositions,
				textMissingPositions,
				numMissingDRPs,
				textMissingDRPs
			));
		}
		else if (numMissingDRPs > 0 && numMissingPositions > 1) {
			results.add(SARS2ValidationMessage.MultiplePositionsMissingWithSingleDRP.formatWithLevel(
				ValidationLevel.WARNING,
				numMissingPositions,
				textMissingPositions,
				textMissingDRPs
			));
		}
		else if (numMissingDRPs > 0) {
			results.add(SARS2ValidationMessage.SingleDRPMissing.formatWithLevel(
				ValidationLevel.NOTE,
				textMissingDRPs
			));
		}
		else if (numMissingPositions > 1) {
			results.add(SARS2ValidationMessage.MultiplePositionsMissingWithoutDRP.formatWithLevel(
				ValidationLevel.WARNING,
				numMissingPositions,
				textMissingPositions
			));
		}
		else if (numMissingPositions > 0) {
			results.add(SARS2ValidationMessage.SinglePositionMissingWithoutDRP.formatWithLevel(
				ValidationLevel.NOTE,
				textMissingPositions
			));
		}
		return results;
	}

	protected static List<ValidationResult> validateNoMissingPositions(
		AlignedSequence<SARS2> alignedSequence,
		Collection<String> includeGenes
	) {
		List<AlignedGeneSeq<SARS2>> geneSeqs = alignedSequence.getAlignedGeneSequences(includeGenes);
		if (geneSeqs.isEmpty()) {
			return Collections.emptyList();
		}
		AlignedGeneSeq<SARS2> geneSeq = geneSeqs.get(0);
		GenePosition<SARS2> leftMost = new GenePosition<>(geneSeq.getGene(), 1);
		geneSeq = geneSeqs.get(geneSeqs.size() - 1);
		GenePosition<SARS2> rightMost = new GenePosition<>(geneSeq.getGene(), geneSeq.getGene().getAASize());
		Strain<SARS2> strain = alignedSequence.getStrain();
		Map<Gene<SARS2>, GeneRegions<SARS2>> unseqRegions = includeGenes.stream()
			.map(absGene -> strain.getGene(absGene))
			.collect(Collectors.toMap(
				gene -> gene,
				gene -> {
					AlignedGeneSeq<SARS2> gs = alignedSequence.getAlignedGeneSequence(gene);
					return gs == null ? (
						GeneRegions.newGeneRegions(gene, 1, gene.getAASize())
					) : gs.getUnsequencedRegions();
				}
			));
			
		Set<GenePosition<SARS2>> needGenePositions = GenePosition
			.getGenePositionsBetween(leftMost, rightMost, includeGenes);
		
		// For DRPs, the leftMost must be the begining of the first gene and the rightMost must be the ending of the last gene
		Set<GenePosition<SARS2>> needDRGenePositions = GenePosition
			.getDRGenePositionsBetween(leftMost, rightMost, includeGenes);

		Set<GenePosition<SARS2>> availableGenePositions = needGenePositions.stream()
			.filter(gpos -> {
				Gene<SARS2> gene = gpos.getGene();
				GeneRegions<SARS2> geneUnseqRegions = unseqRegions.get(gene);
				if (geneUnseqRegions == null) {
					return true;
				}
				if (geneUnseqRegions.contains(gpos.getPosition())) {
					return false;
				}
				return true;
			})	
			.collect(Collectors.toSet());
		return validateNoMissingPositions(
			needGenePositions,
			needDRGenePositions,
			availableGenePositions
		);
	}

	protected static List<ValidationResult> validateLongGap(
		AlignedSequence<SARS2> alignedSequence,
		Collection<String> includeGenes
	) {
		int gapLenThreshold = 20;
		int totalIndels = 0;
		List<ValidationResult> result = new ArrayList<>();
		for (Mutation<SARS2> mut : alignedSequence.getMutations()) {
			if (!includeGenes.contains(mut.getAbstractGene())) {
				continue;
			}
			if (totalIndels > gapLenThreshold) {
				result.add(SARS2ValidationMessage.FASTAGapTooLong.format());
				break;
			}
			if (mut.getInsertedNAs().length() > gapLenThreshold * 3) {
				result.add(SARS2ValidationMessage.FASTAGapTooLong.format());
				break;
			}
			if (mut.isDeletion()) {
				totalIndels ++;
			}
			else if (mut.isInsertion()) {
				totalIndels += Math.round(mut.getInsertedNAs().length() / 3);
			}
			else {
				totalIndels = 0;
			}
		}
		return result;
	}

	protected static List<ValidationResult> validateNAs(AlignedSequence<SARS2> alignedSequence) {
		List<String> invalids =
			alignedSequence.getInputSequence().removedInvalidChars()
			.stream().map(c -> "" + c)
			.collect(Collectors.toList());
		List<ValidationResult> result = new ArrayList<>();
		if (!invalids.isEmpty()) {
			result.add(SARS2ValidationMessage.FASTAInvalidNAsRemoved.format(
				Json.dumps(String.join("", invalids))
			));
		}
		return result;
	}

	protected static List<ValidationResult> validateNoStopCodons(
		AlignedSequence<SARS2> alignedSequence,
		Collection<String> includeGenes
	) {
		List<ValidationResult> results = new ArrayList<>();
		MutationSet<SARS2> stopCodons = (
			alignedSequence.getMutations()
			.getStopCodons()
			.filterBy(mut -> includeGenes.contains(mut.getAbstractGene()))
		);
		for (Map.Entry<Gene<SARS2>, MutationSet<SARS2>> entry : stopCodons.groupByGene().entrySet()) {
			String geneDisplay = entry.getKey().getDisplay();
			MutationSet<SARS2> geneStopCodons = entry.getValue();
			int numGeneStopCodons = geneStopCodons.size();
			String geneStopText = geneStopCodons.join(", ", Mutation::getHumanFormat);
			if (numGeneStopCodons > 1) {
				results.add(SARS2ValidationMessage.MultipleStopCodons.formatWithLevel(
					ValidationLevel.SEVERE_WARNING,
					numGeneStopCodons,
					geneDisplay,
					geneStopText
				));
			} else if (numGeneStopCodons > 0) {
				results.add(SARS2ValidationMessage.SingleStopCodon.formatWithLevel(
					ValidationLevel.NOTE,
					geneDisplay,
					geneStopText
				));
			}
		}
		
		return results;
	}

	protected static List<ValidationResult> validateNoTooManyUnusualMutations(
		AlignedSequence<SARS2> alignedSequence,
		Collection<String> includeGenes
	) {
		return SARS2DefaultMutationsValidator.validateNoTooManyUnusualMutations(alignedSequence.getMutations(), includeGenes);
	}

	private static List<ValidationResult> validateGaps(
		AlignedSequence<SARS2> alignedSequence,
		Collection<String> includeGenes
	) {
		Map<Gene<SARS2>, AlignedGeneSeq<SARS2>> alignedGeneSeqs = alignedSequence.getAlignedGeneSequenceMap();
		List<Gene<SARS2>> seqGenes = alignedSequence.getAvailableGenes();
		List<ValidationResult> results = new ArrayList<>();

		for (Gene<SARS2> gene : seqGenes) {
			String geneName = gene.getAbstractGene();
			if (!includeGenes.contains(geneName)) {
				continue;
			}
			String geneDisplay = gene.getDisplay();
			AlignedGeneSeq<SARS2> alignedGeneSeq = alignedGeneSeqs.get(gene);
			List<FrameShift<SARS2>> frameShifts = alignedGeneSeq.getFrameShifts();
			MutationSet<SARS2> insertions = alignedGeneSeq.getInsertions();
			MutationSet<SARS2> deletions = alignedGeneSeq.getDeletions();
			MutationSet<SARS2> unusualInsertions = insertions.getUnusualMutations();
			MutationSet<SARS2> unusualDeletions = deletions.getUnusualMutations();
			MutationSet<SARS2> unusualIndels = unusualInsertions.mergesWith(unusualDeletions);
			int numTotal = frameShifts.size() + unusualInsertions.size() + unusualDeletions.size();
			String frameShiftListText = FrameShift.joinFrameShifts(frameShifts);
			String unusualIndelsListText = unusualIndels.join(", ");

			if (numTotal > 1) {
				if (frameShifts.size() > 0 && unusualIndels.size() > 0) {
					results.add(SARS2ValidationMessage.MultipleUnusualIndelsAndFrameshifts.formatWithLevel(
						ValidationLevel.SEVERE_WARNING,
						geneDisplay,
						numTotal,
						unusualIndelsListText,
						frameShiftListText
					));
				} else if (frameShifts.size() > 0) {
					results.add(SARS2ValidationMessage.MultipleFrameShifts.formatWithLevel(
						ValidationLevel.SEVERE_WARNING,
						geneDisplay,
						numTotal,
						frameShiftListText
					));
				} else {
					results.add(SARS2ValidationMessage.MultipleUnusualIndels.formatWithLevel(
						ValidationLevel.SEVERE_WARNING,
						geneDisplay,
						numTotal,
						unusualIndelsListText
					));
				}

			} else if (numTotal >0 ) {
				if (frameShifts.size() > 0) {
					results.add(SARS2ValidationMessage.SingleFrameshift.formatWithLevel(
						ValidationLevel.WARNING,
						geneDisplay,
						frameShiftListText
					));
				} else {
					results.add(SARS2ValidationMessage.SingleUnusualIndel.formatWithLevel(
						ValidationLevel.WARNING,
						geneDisplay,
						unusualIndelsListText
					));
				}

			}
		}
		return results;
	}

}
