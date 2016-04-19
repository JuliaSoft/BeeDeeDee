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
 * A wrapper for a GER representation using the {@link BDD} interface.
 */
public class BDDGer implements BDD {

	private GER ger;

	/**
	 * Constructs a BDDGer by normalizing the given bdd.
	 * 
	 * @param bdd the starting BDD, which is immediately freed if not equal to
	 *            (same as) normalized
	 */
	BDDGer(BDD bdd) {
		if (bdd != null) {
			GER temp = new GER(bdd);
			ger = temp.normalize();
			if (!bdd.equals(ger.getN())) {
				temp.free();
			}
		}
	}

	private BDDGer(GER ger) {
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
		if (!(other instanceof BDDGer)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDGer otherBddGer = (BDDGer) other;
		GER orGer = ger.or(otherBddGer.ger);

		return new BDDGer(orGer);
	}

	@Override
	public BDD orWith(BDD other) {
		if (!(other instanceof BDDGer)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDGer otherBddGer = (BDDGer) other;
		GER orGer = ger.or(otherBddGer.ger);
		free();
		otherBddGer.ger.free();
		ger = orGer;

		return this;
	}

	@Override
	public BDD and(BDD other) {
		if (!(other instanceof BDDGer)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDGer otherBddGer = (BDDGer) other;
		GER andGer = ger.and(otherBddGer.ger);

		return new BDDGer(andGer);
	}

	@Override
	public BDD andWith(BDD other) {
		if (!(other instanceof BDDGer)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDGer otherBddGer = (BDDGer) other;
		GER andGer = ger.and(otherBddGer.ger);
		free();
		otherBddGer.ger.free();
		ger = andGer;

		return this;
	}

	@Override
	public BDD xor(BDD other) {
		if (!(other instanceof BDDGer)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDGer otherBddGer = (BDDGer) other;
		GER xorGer = ger.xor(otherBddGer.ger);

		return new BDDGer(xorGer);
	}

	@Override
	public BDD xorWith(BDD other) {
		if (!(other instanceof BDDGer)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDGer otherBddGer = (BDDGer) other;
		GER xorGer = ger.xor(otherBddGer.ger);
		free();
		otherBddGer.ger.free();
		ger = xorGer;

		return this;
	}

	@Override
	public BDD nand(BDD other) {
		if (!(other instanceof BDDGer)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDGer otherBddGer = (BDDGer) other;
		GER andGer = ger.and(otherBddGer.ger);
		GER not = andGer.not();
		andGer.free();

		return new BDDGer(not);
	}

	@Override
	public BDD nandWith(BDD other) {
		if (!(other instanceof BDDGer)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDGer otherBddGer = (BDDGer) other;
		GER andGer = ger.and(otherBddGer.ger);
		free();
		otherBddGer.ger.free();
		ger = andGer.not();
		andGer.free();

		return this;
	}

	@Override
	public BDD not() {
		return new BDDGer(ger.not());
	}

	@Override
	public BDD notWith() {
		GER notGer = ger.not();
		free();
		ger = notGer;

		return this;
	}

	@Override
	public BDD imp(BDD other) {
		if (!(other instanceof BDDGer)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDGer otherBddGer = (BDDGer) other;
		GER impGer = ger.imp(otherBddGer.ger);

		return new BDDGer(impGer);
	}

	@Override
	public BDD impWith(BDD other) {
		if (!(other instanceof BDDGer)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDGer otherBddGer = (BDDGer) other;
		GER impGer = ger.imp(otherBddGer.ger);
		free();
		otherBddGer.ger.free();
		ger = impGer;

		return this;
	}

	@Override
	public BDD biimp(BDD other) {
		if (!(other instanceof BDDGer)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDGer otherBddGer = (BDDGer) other;
		GER biimpGer = ger.biimp(otherBddGer.ger);

		return new BDDGer(biimpGer);
	}

	@Override
	public BDD biimpWith(BDD other) {
		if (!(other instanceof BDDGer)) {
			// TODO or convert transparently to BDDGer?
			throw new NotBDDGerException();
		}
		BDDGer otherBddGer = (BDDGer) other;
		GER biimpGer = ger.biimp(otherBddGer.ger);
		free();
		otherBddGer.ger.free();
		ger = biimpGer;

		return this;
	}

	@Override
	public BDD copy() {
		return new BDDGer(ger.copy());
	}

	@Override
	public Assignment anySat() throws UnsatException {
		Assignment anySat = ger.getN().anySat();
		E equiv = ger.getEquiv();
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
		throw new UnsupportedOperationException();
	}

	@Override
	public BDD exist(BDD var) {
		BitSet vars = var.vars();
		// TODO try not to use full bdd
		BDD res = ger.getFullBDD();
		for (int i = vars.nextSetBit(0); i >= 0; i = vars.nextSetBit(i + 1)) {
			BDD temp = res;
			res = res.exist(i);
			if (temp != res) {
				temp.free();
			}
		}

		return new BDDGer(res);
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
		BDD fullBDD = ger.getFullBDD();
		BDD replace = fullBDD.replaceWith(renaming);

		return new BDDGer(replace);
	}

	@Override
	public BDD replaceWith(Map<Integer, Integer> renaming) {
		// TODO try not to use full bdd
		BDD fullBDD = ger.getFullBDD();
		BDD replace = fullBDD.replaceWith(renaming);
		GER replaceGer = new GER(replace);
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
		if (other instanceof BDDGer) {
			BDDGer bddGer = (BDDGer) other;
			other = bddGer.ger.getFullBDD();
		}
		BDD fullBDD = ger.getFullBDD();
		boolean equivalentTo = fullBDD.isEquivalentTo(other);
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
		return new BDDGer(high);
	}

	@Override
	public BDD low() {
		BDD fullBDD = ger.getFullBDD();
		BDD low = fullBDD.low();
		fullBDD.free();
		return new BDDGer(low);
	}

	@Override
	public BDD squeezeEquiv(LeaderFunction leaderFunction) {
		throw new UnsupportedOperationException();
	}

	@Override
	public BDD squeezeEquivWith(LeaderFunction leaderFunction) {
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
	public BDD renameWithLeader(E r) {
		return new BDDGer(ger.getFullBDD().renameWithLeader(r));
	}

	@Override
	public Set<Pair> equivVars() {
		return ger.getFullBDD().equivVars();
	}
}
