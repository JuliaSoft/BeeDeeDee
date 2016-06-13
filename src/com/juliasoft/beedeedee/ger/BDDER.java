package com.juliasoft.beedeedee.ger;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.juliasoft.beedeedee.bdd.Assignment;
import com.juliasoft.beedeedee.bdd.BDD;
import com.juliasoft.beedeedee.bdd.UnsatException;
import com.juliasoft.beedeedee.factories.Factory;

/**
 * A wrapper for a ER representation using the {@link BDD} interface.
 */
public class BDDER implements BDD {

	private ER ger;

	/**
	 * Constructs a BDDER by normalizing the given bdd.
	 * 
	 * @param bdd the starting BDD, which is immediately freed if not equal to
	 *            (same as) normalized
	 */
	BDDER(BDD bdd) {
		if (bdd != null) {
			ER temp = new ER(bdd);
			ger = temp.normalize();
			if (!bdd.equals(ger.getN())) {
				temp.free();
			}
		}
	}

	private BDDER(ER ger) {
		this.ger = ger;
	}

	@Override
	public void free() {
		ger.free();
	}

	@Override
	public boolean isZero() {
		BDD fullBDD = ger.getFullBDD();
		boolean zero = fullBDD.isZero();
		fullBDD.free();
		return zero;
	}

	@Override
	public boolean isOne() {
		BDD fullBDD = ger.getFullBDD();
		boolean one = fullBDD.isOne();
		fullBDD.free();
		return one;
	}

	@Override
	public BDD or(BDD other) {
		if (!(other instanceof BDDER)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDER otherBddGer = (BDDER) other;
		ER orGer = ger.or(otherBddGer.ger);

		return new BDDER(orGer);
	}

	@Override
	public BDD orWith(BDD other) {
		if (!(other instanceof BDDER)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDER otherBddGer = (BDDER) other;
		ER orGer = ger.or(otherBddGer.ger);
		free();
		otherBddGer.ger.free();
		ger = orGer;

		return this;
	}

	@Override
	public BDD and(BDD other) {
		if (!(other instanceof BDDER)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDER otherBddGer = (BDDER) other;
		ER andGer = ger.and(otherBddGer.ger);

		return new BDDER(andGer);
	}

	@Override
	public BDD andWith(BDD other) {
		if (!(other instanceof BDDER)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDER otherBddGer = (BDDER) other;
		ER andGer = ger.and(otherBddGer.ger);
		free();
		otherBddGer.ger.free();
		ger = andGer;

		return this;
	}

	@Override
	public BDD xor(BDD other) {
		if (!(other instanceof BDDER)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDER otherBddGer = (BDDER) other;
		ER xorGer = ger.xor(otherBddGer.ger);

		return new BDDER(xorGer);
	}

	@Override
	public BDD xorWith(BDD other) {
		if (!(other instanceof BDDER)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDER otherBddGer = (BDDER) other;
		ER xorGer = ger.xor(otherBddGer.ger);
		free();
		otherBddGer.ger.free();
		ger = xorGer;

		return this;
	}

	@Override
	public BDD nand(BDD other) {
		if (!(other instanceof BDDER)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDER otherBddGer = (BDDER) other;
		ER andGer = ger.and(otherBddGer.ger);
		ER not = andGer.not();
		andGer.free();

		return new BDDER(not);
	}

	@Override
	public BDD nandWith(BDD other) {
		if (!(other instanceof BDDER)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDER otherBddGer = (BDDER) other;
		ER andGer = ger.and(otherBddGer.ger);
		free();
		otherBddGer.ger.free();
		ger = andGer.not();
		andGer.free();

		return this;
	}

	@Override
	public BDD not() {
		return new BDDER(ger.not());
	}

	@Override
	public BDD notWith() {
		ER notGer = ger.not();
		free();
		ger = notGer;

		return this;
	}

	@Override
	public BDD imp(BDD other) {
		if (!(other instanceof BDDER)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDER otherBddGer = (BDDER) other;
		ER impGer = ger.imp(otherBddGer.ger);

		return new BDDER(impGer);
	}

	@Override
	public BDD impWith(BDD other) {
		if (!(other instanceof BDDER)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDER otherBddGer = (BDDER) other;
		ER impGer = ger.imp(otherBddGer.ger);
		free();
		otherBddGer.ger.free();
		ger = impGer;

		return this;
	}

	@Override
	public BDD biimp(BDD other) {
		if (!(other instanceof BDDER)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDER otherBddGer = (BDDER) other;
		ER biimpGer = ger.biimp(otherBddGer.ger);

		return new BDDER(biimpGer);
	}

	@Override
	public BDD biimpWith(BDD other) {
		if (!(other instanceof BDDER)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDER otherBddGer = (BDDER) other;
		ER biimpGer = ger.biimp(otherBddGer.ger);
		free();
		otherBddGer.ger.free();
		ger = biimpGer;

		return this;
	}

	@Override
	public BDD copy() {
		return new BDDER(ger.copy());
	}

	@Override
	public Assignment anySat() throws UnsatException {
		Assignment anySat = ger.getN().anySat();
		EquivalenceRelation equiv = ger.getEquiv();
		equiv.updateAssignment(anySat);
		return anySat;
	}

	@Override
	public List<Assignment> allSat() {
		throw new UnsupportedOperationException();
	}

	@Override
	public long satCount() {
		return ger.satCount();
	}

	@Override
	public long satCount(int maxVar) {
		// TODO FIXME check this
		return ger.satCount();
	}

	@Override
	public BDD restrict(int var, boolean value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public BDD restrict(BDD var) {
		throw new UnsupportedOperationException();
	}

	@Override
	public BDD restrictWith(BDD var) {
		throw new UnsupportedOperationException();
	}

	@Override
	public BDD exist(int var) {
		ER existGer = ger.exist(var);

		return new BDDER(existGer);
	}

	@Override
	public BDD exist(BDD var) {
		ER existGer = ger.exist(var.vars());

		return new BDDER(existGer);
	}

	@Override
	public BDD exist(BitSet var) {
		ER existGer = ger.exist(var);

		return new BDDER(existGer);
	}

	@Override
	public BDD forAll(int var) {
		throw new UnsupportedOperationException();
	}

	@Override
	public BDD forAll(BDD var) {
		throw new UnsupportedOperationException();
	}

	@Override
	public BDD simplify(BDD d) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int[] varProfile() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int nodeCount() {
		return ger.getN().nodeCount();
	}

	@Override
	public BDD replace(Map<Integer, Integer> renaming) {
		// TODO try not to use full bdd
//		BDD fullBDD = ger.getFullBDD();
//		BDD replace = fullBDD.replaceWith(renaming);
		ER replace = ger.replace(renaming);
		return new BDDER(replace);
	}

	@Override
	public BDD replaceWith(Map<Integer, Integer> renaming) {
		// TODO try not to use full bdd
//		BDD fullBDD = ger.getFullBDD();
//		BDD replace = fullBDD.replaceWith(renaming);
		ER replaceGer = ger.replace(renaming);//new GER(replace);
		free();
		ger = replaceGer.normalize();
		replaceGer.free();

		return this;
	}

	@Override
	public long pathCount() {
		throw new UnsupportedOperationException();
	}

	@Override
	public BDD ite(BDD thenBDD, BDD elseBDD) {
		throw new UnsupportedOperationException();
	}

	@Override
	public BDD relProd(BDD other, BDD var) {
		throw new UnsupportedOperationException();
	}

	@Override
	public BDD compose(BDD other, int var) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEquivalentTo(BDD other) {
		if (other instanceof BDDER) {
			BDDER bddGer = (BDDER) other;
			other = bddGer.ger.getFullBDD();
		}
		BDD fullBDD = ger.getFullBDD();
		boolean equivalentTo = fullBDD.isEquivalentTo(other); // FIXME doesn't work if different factory (ids don't match)
		fullBDD.free();
		other.free();
		return equivalentTo;
	}

	@Override
	public int hashCodeAux() {
		return hashCode();
	}

	@Override
	public int var() {
		BDD fullBDD = ger.getFullBDD();
		int var = fullBDD.var();
		fullBDD.free();
		return var;
	}

	@Override
	public BDD high() {
		BDD fullBDD = ger.getFullBDD();
		BDD high = fullBDD.high();
		fullBDD.free();
		return new BDDER(high);
	}

	@Override
	public BDD low() {
		BDD fullBDD = ger.getFullBDD();
		BDD low = fullBDD.low();
		fullBDD.free();
		return new BDDER(low);
	}

	@Override
	public BDD squeezeEquiv(EquivalenceRelation r) {
		throw new UnsupportedOperationException();
	}

	@Override
	public BDD squeezeEquivWith(EquivalenceRelation r) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Factory getFactory() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return ger.toString();
	}

	@Override
	public BitSet vars() {
		return ger.vars();
	}

	@Override
	public int maxVar() {
		return ger.maxVar();
	}

	@Override
	public BDD renameWithLeader(EquivalenceRelation r) {
		return new BDDER(ger.getFullBDD().renameWithLeader(r));
	}

	@Override
	public Set<Pair> equivVars() {
		return ger.getFullBDD().equivVars();
	}
}