import org.apache.commons.codec.digest.DigestUtils;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class CompressionTree {

    private String root;

    private HashMap<String, String> subjects;
    private HashMap<String, ArrayList<PredicateTree>> predicate_trees;

    private HashMap<String, HashMap<String, ArrayList<String>>> adjacency_lists;

    public CompressionTree(RDFTriple[] triples) {
        adjacency_lists = new HashMap<>();
        subjects = new HashMap<>();
        predicate_trees = new HashMap<>();

        generate_adjacency_lists(triples);
        generate_hashed_adjacency_lists();
        generateCompressionTree();
    }

    private void generateCompressionTree() {
        for (String subject: adjacency_lists.keySet()) {

            HashMap<String, ArrayList<String>> predicate_tree = adjacency_lists.get(subject);

            ArrayList<PredicateTree> predicateTrees = new ArrayList<>();

            for (String predicate: predicate_tree.keySet()) {
                // Update predicate trees
                ObjectList objectList = new ObjectList(predicate_tree.get(predicate));
                PredicateTree predicateTree = new PredicateTree(predicate, objectList);
                predicateTrees.add(predicateTree);
            }

            predicate_trees.put(subject, predicateTrees);

            String subject_children_hash = getPredicateTreeDigest(predicate_trees.get(subject));
            subjects.put(subject, subject_children_hash);
        }

        updateRoot();
    }

    public void insertHashedTriple(String hashed_subject, String hashed_predicate, String hashed_object) {
        boolean subject_exists = subjects.containsKey(hashed_subject);

        if (!subject_exists) {
            ObjectList objectList = new ObjectList(hashed_object);
            PredicateTree predicateTree = new PredicateTree(hashed_predicate, objectList);
            ArrayList<PredicateTree> predicateTrees = new ArrayList<>();
            predicateTrees.add(predicateTree);
            predicate_trees.put(hashed_subject, predicateTrees);
        } else {
            boolean predicate_exists = predicateExists(hashed_subject, hashed_predicate);

            if (!predicate_exists) {
                ObjectList objectList = new ObjectList(hashed_object);
                PredicateTree predicateTree = new PredicateTree(hashed_predicate, objectList);
                predicate_trees.get(hashed_subject).add(predicateTree);
            } else {
                PredicateTree predicateTree = getPredicateTree(hashed_subject, hashed_predicate);
                boolean object_exists = predicateTree.getObject_list().contains(hashed_object);

                if (!object_exists) {
                    predicateTree.getObject_list().getList().add(hashed_object);
                    predicateTree.updateChildrenHash();
                }
            }
        }

        String predicate_tree_digest = getPredicateTreeDigest(predicate_trees.get(hashed_subject));
        subjects.put(hashed_subject, predicate_tree_digest);

        updateRoot();
    }

    private PredicateTree getPredicateTree(String hashed_subject, String hashed_predicate) {
        for (PredicateTree predicateTree: predicate_trees.get(hashed_subject)) {
            if (predicateTree.getPredicate().equals(hashed_predicate)) {
                return predicateTree;
            }
        }

        return null;
    }

    private boolean predicateExists(String hashed_subject, String hashed_predicate) {
        for (PredicateTree predicateTree: predicate_trees.get(hashed_subject)) {
            if (predicateTree.getPredicate().equals(hashed_predicate)) {
                return true;
            }
        }

        return false;
    }

    private void updateRoot() {
        root = getDigest(subjects);
    }

    private String getPredicateTreeDigest(ArrayList<PredicateTree> predicateTrees) {
        StringBuilder sb = new StringBuilder();

        for (PredicateTree predicateTree: predicateTrees) {
            sb.append(predicateTree.getDigest());
        }

        return DigestUtils.sha256Hex(sb.toString());
    }

    private void generate_adjacency_lists(RDFTriple[] triples) {
        for (RDFTriple triple: triples) {
            String subject = triple.getSubject();
            String predicate = triple.getPredicate();
            String object = triple.getObject();

            HashMap<String, ArrayList<String>> predicate_tree = adjacency_lists.get(subject);

            if (predicate_tree == null) {
                adjacency_lists.put(subject, getPredicateSubTree(predicate, object));
            } else {
                ArrayList<String> object_list = predicate_tree.get(predicate);

                if (object_list == null) {
                    predicate_tree.put(predicate, getObjectList(object));
                } else {
                    boolean object_exists = object_list.contains(object);

                    if (!object_exists) {
                        object_list.add(object);
                    }
                }
            }
        }
    }

    private void generate_hashed_adjacency_lists() {
        HashMap<String, HashMap<String, ArrayList<String>>> hashed_adjacency_list = new HashMap<>();

        for (String subject: adjacency_lists.keySet()) {
            HashMap<String, ArrayList<String>> hashed_predicate_map = new HashMap<>();

            for (String predicate: adjacency_lists.get(subject).keySet()) {
                ArrayList<String> hashed_object_list = new ArrayList<>();

                for (String object: adjacency_lists.get(subject).get(predicate)) {
                    hashed_object_list.add(DigestUtils.sha256Hex(object));
                }

                hashed_predicate_map.put(DigestUtils.sha256Hex(predicate), hashed_object_list);
            }

            hashed_adjacency_list.put(DigestUtils.sha256Hex(subject), hashed_predicate_map);
        }

        adjacency_lists = hashed_adjacency_list;
    }

    private HashMap<String, ArrayList<String>> getPredicateSubTree(String p, String o) {
        ArrayList<String> object_list = new ArrayList<>();
        object_list.add(o);

        HashMap<String, ArrayList<String>> predicate_tree = new HashMap<>();
        predicate_tree.put(p, object_list);

        return predicate_tree;
    }

    private ArrayList<String> getObjectList(String o) {
        ArrayList<String> object_list = new ArrayList<>();
        object_list.add(o);
        return object_list;
    }

    private String getDigest(HashMap<String, String> map) {
        StringBuilder sb = new StringBuilder();

        for (String key: map.keySet()) {
            String digest = DigestUtils.sha256Hex(key+map.get(key));
            sb.append(digest);
        }

        return DigestUtils.sha256Hex(sb.toString());
    }


    private String getDigest(ArrayList<String> list) {
        StringBuilder sb = new StringBuilder();

        for (String element : list) {
            sb.append(element);
        }

        return DigestUtils.sha256Hex(sb.toString());
    }

    private String getDigest(Set<String> set) {
        StringBuilder sb = new StringBuilder();

        for (String element : set) {
            sb.append(element);
        }

        return DigestUtils.sha256Hex(sb.toString());
    }

    private String getHashTupleListDigest(ArrayList<HashTuple> hashTuples) {
        StringBuilder sb = new StringBuilder();

        for (HashTuple hashTuple: hashTuples) {
            String digest = DigestUtils.sha256Hex(hashTuple.getElementHash()+hashTuple.getChildrenHash());
            sb.append(digest);
        }

        return DigestUtils.sha256Hex(sb.toString());
    }

    public int numberOfSubjects() {
        return subjects.size();
    }

    public int avgNbPredicatesPerSubject() {
        int total_number_predicates = 0;
        for (String subject: adjacency_lists.keySet()) {
            total_number_predicates += adjacency_lists.get(subject).keySet().size();
        }

        return total_number_predicates/numberOfSubjects();
    }

    public String getRoot() {
        return root;
    }

    private boolean containsHashedTriple(RDFTriple triple) {
        String subject = triple.getSubject();
        String predicate = triple.getPredicate();
        String object = triple.getObject();

        boolean subject_exists = subjects.containsKey(subject);

        if (subject_exists) {
            ArrayList<PredicateTree> predicateTrees = predicate_trees.get(subject);

            for (PredicateTree predicateTree: predicateTrees) {
                if (predicateTree.getPredicate().equals(predicate)) {
                    return predicateTree.getObject_list().getList().contains(object);
                }
            }
        }

        return false;
    }

    public Path getPathForHashedTriple(RDFTriple triple) {
        String subject = triple.getSubject();
        String predicate = triple.getPredicate();
        String object = triple.getObject();

        if (containsHashedTriple(triple)) {
            ArrayList<String> subject_list = new ArrayList<>(subjects.keySet());
            ArrayList<String> subject_children_hashes = new ArrayList<>(subjects.values());
            int index_of_subject = subject_list.indexOf(subject);
            subject_list.remove(subject);
            subject_children_hashes.remove(index_of_subject);

            ArrayList<HashTuple> subject_siblings = new ArrayList<>();

            for (int i = 0; i < subject_list.size(); i++) {
                subject_siblings.add(new HashTuple(subject_list.get(i), subject_children_hashes.get(i)));
            }

            ArrayList<String> predicate_list = new ArrayList<>();
            ArrayList<String> predicate_children_hashes = new ArrayList<>();
            ArrayList<String> object_list = null;

            for (PredicateTree predicate_tree: predicate_trees.get(subject)) {
                predicate_list.add(predicate_tree.getPredicate());
                predicate_children_hashes.add(predicate_tree.getChildren_hash());
                if (predicate_tree.getPredicate().equals(predicate)) {
                    object_list = predicate_tree.getObject_list().getList();
                }
            }

            int index_of_predicate = predicate_list.indexOf(predicate);
            predicate_list.remove(predicate);
            predicate_children_hashes.remove(index_of_predicate);

            ArrayList<HashTuple> predicate_siblings = new ArrayList<>();
            for (int i = 0; i < predicate_list.size(); i++) {
                predicate_siblings.add(new HashTuple(predicate_list.get(i), predicate_children_hashes.get(i)));
            }

            int index_of_object = object_list.indexOf(object);
            object_list.remove(object);

            return new Path(subject_siblings, predicate_siblings, object_list, index_of_subject, index_of_predicate, index_of_object);
        }

        return null;
    }

    public boolean hashedTripleExists(RDFTriple triple, String root, Path path) {
        String subject = triple.getSubject();
        String predicate = triple.getPredicate();
        String object = triple.getObject();

        ArrayList<String> object_list = path.getObject_siblings();
        ArrayList<HashTuple> predicate_list = path.getPredicate_siblings();
        ArrayList<HashTuple> subject_list = path.getSubject_siblings();

        object_list.add(path.getObject_index(), object);
        String predicate_children_hash = getDigest(object_list);
        predicate_list.add(path.getPredicate_index(), new HashTuple(predicate, predicate_children_hash));

        String subject_children_hash = getHashTupleListDigest(predicate_list);
        subject_list.add(path.getSubject_index(), new HashTuple(subject, subject_children_hash));

        String reconstructed_root = getHashTupleListDigest(subject_list);

        return root.equals(reconstructed_root);
    }

    public void printAdjacencyList() {
        for (String subject: adjacency_lists.keySet()) {
            System.out.println("Subject: " + subject);

            for (String predicate: adjacency_lists.get(subject).keySet()) {
                System.out.println("\t\tPredicate: " + predicate);

                for (String object: adjacency_lists.get(subject).get(predicate)) {
                    System.out.println("\t\t\t\tObject: " + object);
                }
            }
        }
    }

    public void printTree(int start, int end) {
        if (start == -1 || start > subjects.keySet().size()) {
            start = subjects.keySet().size()-1;
        }

        if (end == -1 || end > subjects.keySet().size()) {
            end = subjects.keySet().size()-1;
        }

        int index = 0;
        System.out.println("Root: " + root);
        for (String subject: subjects.keySet()) {
            if (index >= start) {
                printSubjectTree(subject, index);
            }
            if (index == end) {
                break;
            }
            index++;
        }
    }

    public void printSubjectTree(String subject, int index) {
        System.out.println("Subject " + index + ": (" + subject + ", " + subjects.get(subject) + ")");

        for (int i = 0; i < predicate_trees.get(subject).size(); i++) {
            predicate_trees.get(subject).get(i).print(2, i);
        }
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public HashMap<String, String> getSubjects() {
        return subjects;
    }

    public void setSubjects(HashMap<String, String> subjects) {
        this.subjects = subjects;
    }

    public HashMap<String, ArrayList<PredicateTree>> getPredicate_trees() {
        return predicate_trees;
    }

    public void setPredicate_trees(HashMap<String, ArrayList<PredicateTree>> predicate_trees) {
        this.predicate_trees = predicate_trees;
    }
}
