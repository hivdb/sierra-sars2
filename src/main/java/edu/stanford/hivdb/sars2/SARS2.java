/*

    Copyright (C) 2020 Stanford HIVDB team

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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import edu.stanford.hivdb.comments.ConditionalComments;
import edu.stanford.hivdb.drugresistance.algorithm.DrugResistanceAlgorithm;
import edu.stanford.hivdb.drugs.Drug;
import edu.stanford.hivdb.drugs.DrugClass;
import edu.stanford.hivdb.genotypes.Genotype;
import edu.stanford.hivdb.genotypes.GenotypeReference;
import edu.stanford.hivdb.genotypes.Genotyper;
import edu.stanford.hivdb.mutations.AminoAcidPercents;
import edu.stanford.hivdb.mutations.CodonPercents;
import edu.stanford.hivdb.mutations.GenePosition;
import edu.stanford.hivdb.mutations.Mutation;
import edu.stanford.hivdb.mutations.MutationPrevalence;
import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.mutations.MutationType;
import edu.stanford.hivdb.mutations.MutationTypePair;
import edu.stanford.hivdb.sars2.graphql.SARS2GraphQLExtension;
import edu.stanford.hivdb.seqreads.SequenceReadsAssembler;
import edu.stanford.hivdb.sequences.AlignmentConfig;
import edu.stanford.hivdb.sequences.SequenceAssembler;
import edu.stanford.hivdb.viruses.Gene;
import edu.stanford.hivdb.viruses.Strain;
import edu.stanford.hivdb.viruses.Virus;
import edu.stanford.hivdb.viruses.VirusGraphQLExtension;

public class SARS2 implements Virus<SARS2> {

	private static final String VIRUS_NAME = "SARS2";
	private static final String MAIN_STRAIN = "SARS2";
	private static final String STRAINS_RESPATH = "strains.json";
	private static final String GENES_RESPATH = "genes.json";
	private static final String DRUG_CLASSES_RESPATH = "drug-classes.json";
	private static final String DRUGS_RESPATH = "drugs.json";
	private static final String DRMS_RESPATH = "drms.json";
	private static final String SDRMS_RESPATH = "sdrms.json";
	private static final String TSMS_RESPATH = "tsms.json";
	private static final String APOBECS_RESPATH = "apobecs/apobecs.json";
	private static final String APOBEC_DRMS_RESPATH = "apobecs/apobec_drms.json";
	private static final String AAPCNTS_RESPATH = "aapcnt/rx-%s_taxon-%s.json";
	private static final String CODONPCNTS_RESPATH = "codonpcnt/rx-%s_taxon-%s.json";
	private static final String MUTTYPES_RESPATH = "mutation-types.json";
	private static final String MUTTYPE_PAIRS_RESPATH = "mutation-type-pairs.json";
	private static final String MAIN_SUBTYPES_RESPATH = "main-subtypes.json";
	private static final String GENOTYPE_REFERENCES_RESPATH = "genotypes/genotype-references.json";
	private static final String GENOTYPES_RESPATH = "genotypes/genotypes.json";
	private static final String ALGORITHMS_INDEXPATH = "algorithms/versions.json";
	private static final String ALGORITHMS_RESPATH = "algorithms/%s_%s.xml";
	private static final String CONDCOMMENTS_RESPATH = "conditional-comments.json";
	private static final String ALIGNCONFIG_RESPATH = "alignment-config.json";
	private static final String ASSEMBLYCONFIG_RESPATH = "assembly-config.json";

	static {
		Virus.registerInstance(new SARS2());
	}
		
	public static SARS2 getInstance() {
		return Virus.getInstance(SARS2.class);
	}
	
	private final SARS2DataLoader<SARS2> dl;

	private SARS2() {
		this.dl = new SARS2DataLoader<>(
			this,
			VIRUS_NAME,
			MAIN_STRAIN,
			STRAINS_RESPATH,
			GENES_RESPATH,
			DRUG_CLASSES_RESPATH,
			DRUGS_RESPATH,
			DRMS_RESPATH,
			SDRMS_RESPATH,
			TSMS_RESPATH,
			APOBECS_RESPATH,
			APOBEC_DRMS_RESPATH,
			AAPCNTS_RESPATH,
			CODONPCNTS_RESPATH,
			MUTTYPES_RESPATH,
			MUTTYPE_PAIRS_RESPATH,
			MAIN_SUBTYPES_RESPATH,
			GENOTYPE_REFERENCES_RESPATH,
			GENOTYPES_RESPATH,
			ALGORITHMS_INDEXPATH,
			ALGORITHMS_RESPATH,
			CONDCOMMENTS_RESPATH,
			ALIGNCONFIG_RESPATH,
			ASSEMBLYCONFIG_RESPATH
		);
		registerSequenceValidator(new SARS2DefaultSequenceValidator(this));
		registerMutationsValidator(new SARS2DefaultMutationsValidator());
		registerSequenceReadsValidator(new SARS2DefaultSequenceReadsValidator());
	}

	@Override
	public String getName() {
		return dl.getName();
	}
	
	@Override
	public Strain<SARS2> getMainStrain() {
		return dl.getMainStrain();
	}
	
	@Override
	public Collection<Strain<SARS2>> getStrains() {
		return dl.getStrains();
	}
	
	@Override
	public Strain<SARS2> getStrain(String name) {
		return dl.getStrain(name);
	}

	@Override
	public Collection<Gene<SARS2>> getGenes(Strain<SARS2> strain) {
		return dl.getGenes(strain);
	}
	
	@Override
	public Gene<SARS2> getGene(String name) {
		return dl.getGene(name);
	}
	
	@Override
	public Collection<DrugClass<SARS2>> getDrugClasses() {
		return dl.getDrugClasses();
	}
	
	@Override
	public Map<String, DrugClass<SARS2>> getDrugClassSynonymMap() {
		return dl.getDrugClassSynonymMap();
	}
	
	@Override
	public DrugClass<SARS2> getDrugClass(String name) {
		return dl.getDrugClass(name);
	}
	
	@Override
	public Collection<Drug<SARS2>> getDrugs() {
		return dl.getDrugs();
	}
	
	@Override
	public Map<String, Drug<SARS2>> getDrugSynonymMap() {
		return dl.getDrugSynonymMap();
	}

	@Override
	public Collection<DrugResistanceAlgorithm<SARS2>> getDrugResistAlgorithms() {
		return dl.getDrugResistAlgorithms();
	}

	@Override
	public Collection<DrugResistanceAlgorithm<SARS2>> getDrugResistAlgorithms(Collection<String> algorithmNames) {
		return dl.getDrugResistAlgorithms(algorithmNames);
	}
	
	
	@Override
	public DrugResistanceAlgorithm<SARS2> getDrugResistAlgorithm(String name) {
		return dl.getDrugResistAlgorithm(name);
	}

	@Override
	public DrugResistanceAlgorithm<SARS2> getDrugResistAlgorithm(String family, String version) {
		return dl.getDrugResistAlgorithm(family, version);
	}
	
	@Override
	public Gene<SARS2> extractMutationGene(String mutText) {
		return dl.extractMutationGene(mutText);
	}

	@Override
	public Mutation<SARS2> parseMutationString(Gene<SARS2> defaultGene, String mutText) {
		return dl.parseMutationString(defaultGene, mutText);
	}

	@Override
	public Mutation<SARS2> parseMutationString(String mutText) {
		return dl.parseMutationString(mutText);
	}
	
	@Override
	public MutationSet<SARS2> newMutationSet(String formattedMuts) {
		return dl.newMutationSet(formattedMuts);
	}

	@Override
	public MutationSet<SARS2> newMutationSet(Collection<String> formattedMuts) {
		return dl.newMutationSet(formattedMuts); 
	}

	@Override
	public MutationSet<SARS2> newMutationSet(Gene<SARS2> defaultGene, String formattedMuts) {
		return dl.newMutationSet(defaultGene, formattedMuts);
	}

	@Override
	public MutationSet<SARS2>	newMutationSet(Gene<SARS2> defaultGene, Collection<String> formattedMuts) {
		return dl.newMutationSet(defaultGene, formattedMuts);
	}

	@Override
	public Map<DrugClass<SARS2>, MutationSet<SARS2>> getDrugResistMutations() {
		return dl.getDrugResistMutations();
	}
	
	@Override
	public Map<DrugClass<SARS2>, MutationSet<SARS2>> getSurveilDrugResistMutations() {
		return dl.getSurveilDrugResistMutations();
	}

	@Override
	public Map<DrugClass<SARS2>, MutationSet<SARS2>> getRxSelectedMutations() {
		return dl.getRxSelectedMutations();
	}
	
	@Override
	public MutationSet<SARS2> getApobecMutations() {
		return dl.getApobecMutations();
	}

	@Override
	public MutationSet<SARS2> getApobecDRMs() {
		return dl.getApobecDRMs();
	}

	@Override
	public Collection<MutationType<SARS2>> getMutationTypes() {
		return dl.getMutationTypes();
	}
	
	@Override
	public MutationType<SARS2> getMutationType(String mutTypeText) {
		return dl.getMutationType(mutTypeText);
	}

	@Override
	public Collection<MutationTypePair<SARS2>> getMutationTypePairs() {
		return dl.getMutationTypePairs();
	}
	
	@Override
	public AminoAcidPercents<SARS2> getAminoAcidPercents(Strain<SARS2> strain, String treatment, String subtype) {
		return dl.getAminoAcidPercents(strain, treatment, subtype);
	}

	@Override
	public AminoAcidPercents<SARS2> getMainAminoAcidPercents(Strain<SARS2> strain) {
		return dl.getAminoAcidPercents(strain, "all", "SARS2");
	}

	@Override
	public CodonPercents<SARS2> getCodonPercents(Strain<SARS2> strain, String treatment, String subtype) {
		return dl.getCodonPercents(strain, treatment, subtype);
	}

	@Override
	public CodonPercents<SARS2> getMainCodonPercents(Strain<SARS2> strain) {
		return dl.getCodonPercents(strain, "all", "SARS2");
	}

	@Override
	public List<MutationPrevalence<SARS2>> getMutationPrevalence(GenePosition<SARS2> genePos) {
		return dl.getMutationPrevalence(genePos);
	}
	
	@Override
	public ConditionalComments<SARS2> getConditionalComments() {
		return dl.getConditionalComments();
	}
	
	@Override
	public List<String> getMainSubtypes(Strain<SARS2> strain) {
		return dl.getMainSubtypes(strain);
	}
	
	@Override
	public Map<Gene<SARS2>, Map<String, Integer[]>> getNumPatientsForAAPercents(Strain<SARS2> strain) {
		return dl.getNumPatientsForAAPercents(strain);
	}

	@Override
	public Collection<Genotype<SARS2>> getGenotypes() {
		return dl.getGenotypes();
	}
	
	@Override
	public Genotype<SARS2> getGenotype(String name) {
		return dl.getGenotype(name);
	}

	@Override
	public Genotype<SARS2> getGenotypeUnknown() {
		return dl.getGenotypeUnknown();
	}

	@Override
	public List<GenotypeReference<SARS2>> getGenotypeReferences() {
		return dl.getGenotypeReferences();
	}
	
	@Override
	public Genotyper<SARS2> getGenotyper() {
		return dl.getGenotyper();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) { return true; }
		// SARS2 instance is a singleton
		return false;
	}
	
	@Override
	public DrugResistanceAlgorithm<SARS2> getDefaultDrugResistAlgorithm() {
		return getLatestDrugResistAlgorithm("Stanford-SARS2");
	}
	
	@Override
	public AlignmentConfig<SARS2> getAlignmentConfig() {
		return dl.getAlignmentConfig();
	}
	
	@Override
	public VirusGraphQLExtension getVirusGraphQLExtension() {
		return SARS2GraphQLExtension.getInstance();
	}

	@Override
	public SequenceReadsAssembler<SARS2> getSequenceReadsAssembler(Strain<SARS2> strain) {
		return dl.getSeqReadsAssemblers().get(strain);
	}

	@Override
	public SequenceAssembler<SARS2> getSequenceAssembler(Strain<SARS2> strain) {
		return dl.getSequenceAssemblers().get(strain);
	}

}
