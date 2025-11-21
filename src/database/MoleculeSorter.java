package database;

import java.util.Comparator;

public class MoleculeSorter implements Comparator<Molecule> {
    public static MoleculeSorter DefaultSorter = new MoleculeSorter((o1, o2) -> 0);
    public static MoleculeSorter AlphaSorter = new MoleculeSorter(Comparator.comparing(mol -> mol.molName, MoleculeSorter::alphaCompare));
    public static MoleculeSorter ReverseAlphaSorter = new MoleculeSorter(Comparator.comparing(mol -> mol.molName, (a, b) -> -alphaCompare(a, b)));

    private Comparator<Molecule> comparator;

    public MoleculeSorter(Comparator<Molecule> comparator) {
        this.comparator = comparator;
    }

    private static int alphaCompare(String a, String b) {
        int firstG = alphaGroup(a.charAt(0));
        int secondG = alphaGroup(b.charAt(0));
        if (firstG != secondG) return firstG - secondG;
        return a.compareToIgnoreCase(b) == 0 ? a.compareTo(b) : a.compareToIgnoreCase(b);
    }

    private static int alphaGroup(Character c) {
        return Character.isLetter(c) ? 0 : Character.isDigit(c) ? 1 : 2;
    }

    @Override
    public int compare(Molecule o1, Molecule o2) {
        return comparator.compare(o1, o2);
    }
}
