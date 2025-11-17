package database;

import java.util.Comparator;

public class MoleculeSorter implements Comparator<Molecule> {
    public static MoleculeSorter DefaultSorter = new MoleculeSorter((o1, o2) -> 0);
    public static MoleculeSorter AlphaSorter = new MoleculeSorter(Comparator.comparing(o -> o.molName));
    public static MoleculeSorter ReverseAlphaSorter = new MoleculeSorter((o1, o2) -> o2.molName.compareTo(o1.molName));

    private Comparator<Molecule> comparator;

    public MoleculeSorter(Comparator<Molecule> comparator) {
        this.comparator = comparator;
    }

    @Override
    public int compare(Molecule o1, Molecule o2) {
        return comparator.compare(o1, o2);
    }
}
