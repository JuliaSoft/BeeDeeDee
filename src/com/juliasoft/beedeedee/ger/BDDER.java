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

	private ER er;

	/**
	 * Constructs a BDDER by normalizing the given bdd.
	 * 
	 * @param bdd the starting BDD, which is immediately freed if not equal to
	 *            (same as) normalized
	 */
	BDDER(BDD bdd) {
		if (bdd != null) {
			ER temp = new ER(bdd);
			er = temp.normalize();
			if (!bdd.equals(er.getN())) {
				temp.free();
			}
		}
	}

	private BDDER(ER er) {
		this.er = er;
	}

	@Override
	public void free() {
		er.free();
	}

	@Override
	public boolean isZero() {
		return er.getN().isZero();
	}

	@Override
	public boolean isOne() {
		return er.getN().isOne() && er.getEquiv().isEmpty();
	}

	@Override
	public BDD or(BDD other) {
		if (!(other instanceof BDDER)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDER otherBddGer = (BDDER) other;
		ER orGer = er.or(otherBddGer.er);

		return new BDDER(orGer);
	}

	@Override
	public BDD orWith(BDD other) {
		if (!(other instanceof BDDER)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDER otherBddGer = (BDDER) other;
		ER orGer = er.or(otherBddGer.er);
		free();
		otherBddGer.er.free();
		er = orGer;

		return this;
	}

	@Override
	public BDD and(BDD other) {
		if (!(other instanceof BDDER)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDER otherBddGer = (BDDER) other;
		ER andGer = er.and(otherBddGer.er);

		return new BDDER(andGer);
	}

	@Override
	public BDD andWith(BDD other) {
		if (!(other instanceof BDDER)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDER otherBddGer = (BDDER) other;
		ER andGer = er.and(otherBddGer.er);
		free();
		otherBddGer.er.free();
		er = andGer;

		return this;
	}

	@Override
	public BDD xor(BDD other) {
		if (!(other instanceof BDDER)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDER otherBddGer = (BDDER) other;
		ER xorGer = er.xor(otherBddGer.er);

		return new BDDER(xorGer);
	}

	@Override
	public BDD xorWith(BDD other) {
		if (!(other instanceof BDDER)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDER otherBddGer = (BDDER) other;
		ER xorGer = er.xor(otherBddGer.er);
		free();
		otherBddGer.er.free();
		er = xorGer;

		return this;
	}

	@Override
	public BDD nand(BDD other) {
		if (!(other instanceof BDDER)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDER otherBddGer = (BDDER) other;
		ER andGer = er.and(otherBddGer.er);
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
		ER andGer = er.and(otherBddGer.er);
		free();
		otherBddGer.er.free();
		er = andGer.not();
		andGer.free();

		return this;
	}

	@Override
	public BDD not() {
		return new BDDER(er.not());
	}

	@Override
	public BDD notWith() {
		ER notGer = er.not();
		free();
		er = notGer;

		return this;
	}

	@Override
	public BDD imp(BDD other) {
		if (!(other instanceof BDDER)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDER otherBddGer = (BDDER) other;
		ER impGer = er.imp(otherBddGer.er);

		return new BDDER(impGer);
	}

	@Override
	public BDD impWith(BDD other) {
		if (!(other instanceof BDDER)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDER otherBddGer = (BDDER) other;
		ER impGer = er.imp(otherBddGer.er);
		free();
		otherBddGer.er.free();
		er = impGer;

		return this;
	}

	@Override
	public BDD biimp(BDD other) {
		if (!(other instanceof BDDER)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDER otherBddGer = (BDDER) other;
		ER biimpGer = er.biimp(otherBddGer.er);

		return new BDDER(biimpGer);
	}

	@Override
	public BDD biimpWith(BDD other) {
		if (!(other instanceof BDDER)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDER otherBddGer = (BDDER) other;
		ER biimpGer = er.biimp(otherBddGer.er);
		free();
		otherBddGer.er.free();
		er = biimpGer;

		return this;
	}

	@Override
	public BDD copy() {
		return new BDDER(er.copy());
	}

	@Override
	public Assignment anySat() throws UnsatException {
		Assignment anySat = er.getN().anySat();
		EquivalenceRelation equiv = er.getEquiv();
		equiv.updateAssignment(anySat);
		return anySat;
	}

	@Override
	public List<Assignment> allSat() {
		throw new UnsupportedOperationException();
	}

	@Override
	public long satCount() {
		return er.satCount();
	}

	@Override
	public long satCount(int maxVar) {
		// TODO FIXME check this
		return er.satCount();
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
		ER existGer = er.exist(var);

		return new BDDER(existGer);
	}

	@Override
	public BDD exist(BDD var) {
		ER existGer = er.exist(var.vars());

		return new BDDER(existGer);
	}

	@Override
	public BDD exist(BitSet var) {
		ER existGer = er.exist(var);

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
		return er.getN().nodeCount();
	}

	@Override
	public BDD replace(Map<Integer, Integer> renaming) {
		ER replace = er.replace(renaming);
		return new BDDER(replace);
	}

	@Override
	public BDD replaceWith(Map<Integer, Integer> renaming) {
		ER replaceGer = er.replace(renaming);
		free();
		er = replaceGer.normalize();
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
		if (!(other instanceof BDDER)) {
			return other.isEquivalentTo(er.getFullBDD());
		}
		BDDER o = (BDDER) other;
		return er.getEquiv().equals(o.er.getEquiv()) && er.getN().isEquivalentTo(o.er.getN());
	}

	@Override
	public int hashCodeAux() {
		return er.getN().hashCodeAux() ^ er.getEquiv().hashCode();
	}

	@Override
	public int var() {
		BDD fullBDD = er.getFullBDD();
		int var = fullBDD.var();
		fullBDD.free();
		return var;
	}

	@Override
	public BDD high() {
		BDD fullBDD = er.getFullBDD();
		BDD high = fullBDD.high();
		fullBDD.free();
		return new BDDER(high);
	}

	@Override
	public BDD low() {
		BDD fullBDD = er.getFullBDD();
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
		return er.toString();
	}

	@Override
	public BitSet vars() {
		return er.vars();
	}

	@Override
	public int maxVar() {
		return er.maxVar();
	}

	@Override
	public BDD renameWithLeader(EquivalenceRelation r) {
		return new BDDER(er.getFullBDD().renameWithLeader(r));
	}

	@Override
	public Set<Pair> equivVars() {
		return er.getFullBDD().equivVars();
	}

	public boolean isNormalized() {
		ER norm = er.normalize();
		return norm.getEquiv().equals(er.getEquiv()) && norm.getN().isEquivalentTo(er.getN());
	}
}
