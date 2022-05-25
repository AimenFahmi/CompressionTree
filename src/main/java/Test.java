
import org.apache.commons.codec.digest.DigestUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.NTriplesDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


public class Test {

    public static void main(String[] args) throws Exception {
        generalTesting();
    }

    public static void generalTesting() throws OWLOntologyStorageException, OWLOntologyCreationException {
        File file = new File("src/main/resources/ico.owl");

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(file);

        System.out.println("\t\tApplying the compression tree method...");

        // Translating from OWL to RDF and measuring the process
        Instant start_translating_owl_to_rdf = Instant.now();
        OWLToRDFTranslator translator = new OWLToRDFTranslator(ontology);
        Instant finish_translating_owl_to_rdf = Instant.now();
        long timeElapsed = Duration.between(start_translating_owl_to_rdf, finish_translating_owl_to_rdf).toMillis();
        System.out.println("\t\t\t\tTranslating from OWL to RDF triples took: " + timeElapsed + "ms");

        RDFTriple[] triples = translator.getTriples();
        System.out.println("\t\t\t\tNumber of triples: " + triples.length);

        // Generating the compression tree and mesuring the process
        Instant start_generating_compression_tree = Instant.now();
        CompressionTree ct = new CompressionTree(triples);
        Instant finish_generating_compression_tree = Instant.now();
        timeElapsed = Duration.between(start_generating_compression_tree, finish_generating_compression_tree).toMillis();

        ct.printTree(0, 1);
        RDFTriple triple = new RDFTriple("f9c63ac674850845994b0035ead68a8f40af5cce1f55dffeab2a12edddef0854", "854298218728d33760b262ef131307db906aa6187bd528941fc0e4a251c990c0", "61b77a8ca20a8f5c535b38cf5e54972b8d1c6e70e064439fc712ac0d0396894e");
        Path path = ct.getPathForHashedTriple(triple);
        System.out.println(ct.hashedTripleExists(triple, ct.getRoot(), path));
        //ct.insertHashedTriple("175202ce66e0b690b47c6bdfbe674f6249145620700256ffd5390870c8f16044", "1bbe446935f611b7f4c84805184a8e8f7184745699bdc877017dc117944d8eef", "786903c96c7739f78c36195d077a4ab1a1b1cd986d2c8a2f38245a6b41bbb151");
        //ct.printTree(0, -1);
        System.out.println("\t\t\t\tGenerating the compression tree took: " + timeElapsed + "ms");
    }

    public static void testingSubjectQuery(CompressionTree ct) {
        HashMap<String, String> subjects = ct.getSubjects();
        Instant start_finding_subject = Instant.now();
        String subject = "c7490f10f23bdeaa864525b061e992860f3a51d0e2202331bf08824a3821254f";
        boolean subject_exists = subjects.containsKey(subject);
        Instant finish_finding_subject = Instant.now();
        long timeElapsed = Duration.between(start_finding_subject, finish_finding_subject).toMillis();
        System.out.println("\t\t\t\tFinding subject " + subject + " takes: " + timeElapsed);
        System.out.println(subject_exists);
    }

    public static CompressionTree getCompressionTree(OWLOntology ontology) throws OWLOntologyStorageException {
        OWLToRDFTranslator translator = new OWLToRDFTranslator(ontology);
        RDFTriple[] triples = translator.getTriples();
        CompressionTree ct = new CompressionTree(triples);
        return ct;
    }


    public static void launchTest() throws OWLOntologyCreationException, OWLOntologyStorageException {
        ArrayList<File> files = new ArrayList<>();

        files.add(new File("src/main/resources/biomodels-21.owl"));
        files.add(new File("src/main/resources/CHV_SKOS_2019.owl"));
        files.add(new File("src/main/resources/gexo.rdf"));
        files.add(new File("src/main/resources/hoom_orphanet.owl"));
        files.add(new File("src/main/resources/mesh.owl"));
        files.add(new File("src/main/resources/pcl.owl"));
        files.add(new File("src/main/resources/reto.rdf"));
        files.add(new File("src/main/resources/rexo.rdf"));
        files.add(new File("src/main/resources/upheno.owl"));
        files.add(new File("src/main/resources/vto.owl"));

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        for (File file : files) {
            System.out.println("\n\n### File: " + file.getName() + "\n");
            Instant start_loading_ontology = Instant.now();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(file);
            Instant finish_loading_ontology = Instant.now();
            long timeElapsed = Duration.between(start_loading_ontology, finish_loading_ontology).toMillis();

            System.out.println("The ontology has " + ontology.getClassesInSignature(Imports.INCLUDED).size() + " classes");
            System.out.println("Loading the ontology took: " + timeElapsed + "ms");

            Instant start_method_1 = Instant.now();
            String digest = compressionTreeMethod(ontology);
            Instant finish_method_1 = Instant.now();
            timeElapsed = Duration.between(start_method_1, finish_method_1).toMillis();
            System.out.println("\t\t\t\tThe entire method took: " + timeElapsed + "ms");

            System.out.println("\t\t\t\tThe ontology's digest is: " + digest);

            Instant start_method_2 = Instant.now();
            digest = simpleHashingMethod(ontology);
            Instant finish_method_2 = Instant.now();
            timeElapsed = Duration.between(start_method_2, finish_method_2).toMillis();
            System.out.println("\t\t\t\tThe entire method took: " + timeElapsed + "ms");

            System.out.println("\t\t\t\tThe ontology's digest is: " + digest);
        }
    }

        public static String compressionTreeMethod (OWLOntology ontology) throws OWLOntologyStorageException {
            System.out.println("\t\tApplying the compression tree method...");

            // Translating from OWL to RDF and measuring the process
            Instant start_translating_owl_to_rdf = Instant.now();
            OWLToRDFTranslator translator = new OWLToRDFTranslator(ontology);
            Instant finish_translating_owl_to_rdf = Instant.now();
            long timeElapsed = Duration.between(start_translating_owl_to_rdf, finish_translating_owl_to_rdf).toMillis();
            System.out.println("\t\t\t\tTranslating from OWL to RDF triples took: " + timeElapsed + "ms");

            RDFTriple[] triples = translator.getTriples();
            System.out.println("\t\t\t\tNumber of triples: " + triples.length);

            // Generating the compression tree and mesuring the process
            Instant start_generating_compression_tree = Instant.now();
            CompressionTree ct = new CompressionTree(triples);
            Instant finish_generating_compression_tree = Instant.now();
            timeElapsed = Duration.between(start_generating_compression_tree, finish_generating_compression_tree).toMillis();
            System.out.println("\t\t\t\tGenerating the compression tree took: " + timeElapsed + "ms");

            int nb_subjects = ct.numberOfSubjects();
            int avg_nb_predicates = ct.avgNbPredicatesPerSubject();

            System.out.println("\t\t\t\tNumber of subjects: " + nb_subjects);
            System.out.println("\t\t\t\tAverage number of predicates: " + avg_nb_predicates);

            return ct.getRoot();
        }

        public static String simpleHashingMethod (OWLOntology ontology) {
            System.out.println("\t\tApplying the simple hashing method...");
            Set<OWLEntity> signature = ontology.getSignature(Imports.INCLUDED);
            Set<OWLAxiom> axioms = ontology.getAxioms(Imports.INCLUDED);
            Set<OWLAnnotation> annotations = ontology.getAnnotations();
            Set<OWLObject> all = new HashSet<>();
            all.addAll(signature);
            all.addAll(annotations);
            all.addAll(axioms);
            return HashStringUtils.boundedHex(all.hashCode());
        }
}
