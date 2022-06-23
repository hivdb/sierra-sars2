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

import com.google.common.collect.Lists;

import edu.stanford.hivdb.mutations.GenePosition;
import edu.stanford.hivdb.mutations.Mutation;
import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.seqreads.OneCodonReadsCoverage;
import edu.stanford.hivdb.seqreads.SequenceReads;
import edu.stanford.hivdb.seqreads.SequenceReadsValidator;
import edu.stanford.hivdb.sequences.GeneRegions;
import edu.stanford.hivdb.sequences.GeneRegions.GeneRegion;
import edu.stanford.hivdb.utilities.MyStringUtils;
import edu.stanford.hivdb.utilities.ValidationLevel;
import edu.stanford.hivdb.utilities.ValidationResult;
import edu.stanford.hivdb.viruses.Gene;
import edu.stanford.hivdb.viruses.Strain;

public class SARS2DefaultSequenceReadsValidator implements SequenceReadsValidator<SARS2> {

	public List<ValidationResult> validate(SequenceReads<SARS2> seqReads, Collection<String> includeGenes) {
		List<ValidationResult> results = new ArrayList<>();
		results.addAll(validateNotEmpty(seqReads, includeGenes));
		if (!results.isEmpty()) {
			return results;
		}
		results.addAll(validateTrimmedPositions(seqReads, includeGenes));
		results.addAll(validateNoMissingPositions(seqReads, includeGenes));
		results.addAll(validateMixtureRateTooHigh(seqReads));
		results.addAll(validateNoStopCodons(seqReads, includeGenes));
		results.addAll(validateNoTooManyUnusualMutations(seqReads, includeGenes));
		return results;
	}

	protected static List<ValidationResult> validateNotEmpty(
		SequenceReads<SARS2> seqReads,
		Collection<String> includeGenes
	) {
		SARS2 virusIns = SARS2.getInstance();
		boolean isNotEmpty = seqReads.getAvailableGenes().stream()
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
	
	protected static List<ValidationResult> validateMixtureRateTooHigh(SequenceReads<SARS2> seqReads) {
		List<ValidationResult> results = new ArrayList<>();
		double mixtureRate = seqReads.getMixtureRate();
		if (mixtureRate >= 0.02) {
			results.add(SARS2ValidationMessage.NGSMixtureRateTooHigh.format(mixtureRate * 100));
		}
		return results;
				
	}
	
	protected static List<ValidationResult> validateTrimmedPositions(
		SequenceReads<SARS2> seqReads,
		Collection<String> includeGenes
	) {
		SARS2 virusIns = SARS2.getInstance();
		Strain<SARS2> strain = virusIns.getMainStrain();
		List<ValidationResult> results = new ArrayList<>();
		List<OneCodonReadsCoverage<SARS2>> crcs = seqReads.getCodonReadsCoverage(includeGenes);
		for (String geneName : includeGenes) {
			Gene<SARS2> gene = strain.getGene(geneName);
			List<OneCodonReadsCoverage<SARS2>> geneCRCs = crcs.stream()
				.filter(crc -> crc.getAbstractGene().equals(geneName))
				.collect(Collectors.toList());
			List<Number> trimmedPos = geneCRCs.stream()
				.filter(crc -> crc.isTrimmed())
				.map(crc -> crc.getPosition())
				.collect(Collectors.toList());
			if (!trimmedPos.isEmpty()) {
				int numTrimmedPos = trimmedPos.size();
				double totalPos = geneCRCs.size();
				double pcnt = (double) numTrimmedPos / totalPos;
				long minReadDepth = seqReads.getMinPositionReads();
				List<GeneRegion> regions = GeneRegions.newGeneRegions(gene, trimmedPos).getRegions();
				results.add(SARS2ValidationMessage.NGSMinReadDepthTooLow.format(
					numTrimmedPos,
					pcnt * 100,
					virusIns.getGeneDisplay(geneName),
					numTrimmedPos == 1 ? "" : "s",
					minReadDepth,
					numTrimmedPos == 1 ? "has" : "have",
					MyStringUtils.andListFormat(regions)
				));
			}
		}
		
		
		return results;
	}

	protected static List<ValidationResult> validateNoMissingPositions(
		SequenceReads<SARS2> seqReads,
		Collection<String> includeGenes
	) {
		List<ValidationResult> results = new ArrayList<>();
		List<OneCodonReadsCoverage<SARS2>> crcs = seqReads.getCodonReadsCoverage(includeGenes).stream()
			.filter(crc -> !crc.isTrimmed())
			.collect(Collectors.toList());
		if (crcs.isEmpty()) {
			return results;
		}
		OneCodonReadsCoverage<SARS2> tmpCRC = crcs.get(0);
		GenePosition<SARS2> leftMost = new GenePosition<>(tmpCRC.getGene(), 1);
		tmpCRC = crcs.get(crcs.size() - 1);
		GenePosition<SARS2> rightMost = new GenePosition<>(tmpCRC.getGene(), tmpCRC.getGene().getAASize());
		
		Set<GenePosition<SARS2>> needGenePositions = GenePosition
			.getGenePositionsBetween(leftMost, rightMost, includeGenes);

		// For DRPs, the leftMost must be the begining of the first gene and the rightMost must be the ending of the last gene
		Set<GenePosition<SARS2>> needDRGenePositions = GenePosition
			.getDRGenePositionsBetween(leftMost, rightMost, includeGenes);

		Set<GenePosition<SARS2>> availableGenePositions = crcs.stream()
			.map(crc -> crc.getGenePosition())
			.collect(Collectors.toSet());
		
		return SARS2DefaultSequenceValidator.validateNoMissingPositions(
				needGenePositions,
				needDRGenePositions,
				availableGenePositions
		);
	}

	protected static List<ValidationResult> validateNoStopCodons(
		SequenceReads<SARS2> seqReads,
		Collection<String> includeGenes
	) {
		List<ValidationResult> results = new ArrayList<>();
		MutationSet<SARS2> stopCodons = (
			seqReads.getMutations()
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
					ValidationLevel.WARNING,
					numGeneStopCodons,
					geneDisplay,
					geneStopText
				));
			} else if (numGeneStopCodons > 0) {
				results.add(SARS2ValidationMessage.SingleStopCodon.formatWithLevel(
					ValidationLevel.WARNING,
					geneDisplay,
					geneStopText
				));
			}
		}
		
		return results;
	}

	protected static List<ValidationResult> validateNoTooManyUnusualMutations(
		SequenceReads<SARS2> seqReads,
		Collection<String> includeGenes
	) {
		return SARS2DefaultMutationsValidator.validateNoTooManyUnusualMutations(seqReads.getMutations(), includeGenes);
	}

}
