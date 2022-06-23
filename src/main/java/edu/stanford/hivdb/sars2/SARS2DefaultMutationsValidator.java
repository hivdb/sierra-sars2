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
import java.util.List;
import java.util.Map;

import edu.stanford.hivdb.mutations.Mutation;
import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.mutations.MutationsValidator;
import edu.stanford.hivdb.utilities.ValidationLevel;
import edu.stanford.hivdb.utilities.ValidationResult;
import edu.stanford.hivdb.viruses.Gene;

public class SARS2DefaultMutationsValidator implements MutationsValidator<SARS2> {

	@Override
	public List<ValidationResult> validate(MutationSet<SARS2> mutations, Collection<String> includeGenes) {
		List<ValidationResult> validationResults = new ArrayList<>();
		validationResults.addAll(validateNoStopCodons(mutations, includeGenes));
		validationResults.addAll(validateNoTooManyUnusualMutations(mutations, includeGenes));
		return validationResults;
	}

	private static List<ValidationResult> validateNoStopCodons(
		MutationSet<SARS2> mutations,
		Collection<String> includeGenes
	) {
		List<ValidationResult> validationResults = new ArrayList<>();
		MutationSet<SARS2> stopCodons = mutations
			.getStopCodons()
			.filterBy(mut -> includeGenes.contains(mut.getAbstractGene()));
		for (Map.Entry<Gene<SARS2>, MutationSet<SARS2>> entry : stopCodons.groupByGene().entrySet()) {
			String geneDisplay = entry.getKey().getDisplay();
			MutationSet<SARS2> geneStopCodons = entry.getValue();
			int numGeneStopCodons = geneStopCodons.size();
			String geneStopText = geneStopCodons.join(", ", Mutation::getHumanFormatWithAbstractGene);
			if (numGeneStopCodons > 1) {
				validationResults.add(SARS2ValidationMessage.MultipleStopCodons.formatWithLevel(
					ValidationLevel.SEVERE_WARNING,
					numGeneStopCodons,
					geneDisplay,
					geneStopText
				));
			} else if (numGeneStopCodons > 0) {
				validationResults.add(SARS2ValidationMessage.SingleStopCodon.formatWithLevel(
					ValidationLevel.WARNING,
					geneDisplay,
					geneStopText
				));
			}
		}
		
		return validationResults;
	}


	protected static List<ValidationResult> validateNoTooManyUnusualMutations(
		MutationSet<SARS2> mutations,
		Collection<String> includeGenes
	) {
		List<ValidationResult> validationResults = new ArrayList<>();
		MutationSet<SARS2> unusualMuts = mutations
			.getUnusualMutations()
			.filterBy(mut -> includeGenes.contains(mut.getAbstractGene()));
		
		MutationSet<SARS2> spikeUnusualMuts = unusualMuts.getGeneMutationsNoSplit("S");
		int numSpikeUnusual = spikeUnusualMuts.size();
		if (numSpikeUnusual >= 10) {
			validationResults.add(SARS2ValidationMessage.MultipleUnusualMutations.format(
				numSpikeUnusual,
				"Spike",
				spikeUnusualMuts.join(", "))
			);
		};
		
		MutationSet<SARS2> rdrpUnusualMuts = unusualMuts.getGeneMutationsNoSplit("RdRP");
		int numRdRPUnusual = rdrpUnusualMuts.size();
		if (numRdRPUnusual >= 5) {
			validationResults.add(SARS2ValidationMessage.MultipleUnusualMutations.format(
				numRdRPUnusual,
				"RdRP",
				rdrpUnusualMuts.join(", "))
			);
		};
		
		MutationSet<SARS2> _3clproUnusualMuts = unusualMuts.getGeneMutationsNoSplit("_3CLpro");
		int num3CLproUnusual = _3clproUnusualMuts.size();
		if (num3CLproUnusual >= 5) {
			validationResults.add(SARS2ValidationMessage.MultipleUnusualMutations.format(
				num3CLproUnusual,
				"3CLpro",
				_3clproUnusualMuts.join(", "))
			);
		};

		return validationResults;

	}

}