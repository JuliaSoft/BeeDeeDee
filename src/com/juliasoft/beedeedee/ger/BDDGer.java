package com.juliasoft.beedeedee.ger;

import java.util.List;
import java.util.Map;

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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BDD impWith(BDD other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BDD biimp(BDD other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BDD biimpWith(BDD other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BDD copy() {
		return new BDDGer(ger.copy());
	}

	@Override
	public Assignment anySat() throws UnsatException {
		Assignment anySat = ger.getN().anySat();
		List<Pair> pairs = ger.getEquiv().pairs();
		for (Pair pair : pairs) {
			anySat.put(pair.first, false);
			anySat.put(pair.second, false);
		}
		return anySat;
	}

	@Override
	public List<Assignment> allSat() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long satCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long satCount(int maxVar) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public BDD restrict(int var, boolean value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BDD restrict(BDD var) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BDD restrictWith(BDD var) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BDD exist(int var) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BDD exist(BDD var) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BDD forAll(int var) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BDD forAll(BDD var) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BDD simplify(BDD d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int[] varProfile() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int nodeCount() {
		return ger.getN().nodeCount();
	}

	@Override
	public BDD replace(Map<Integer, Integer> renaming) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BDD replaceWith(Map<Integer, Integer> renaming) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long pathCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public BDD ite(BDD thenBDD, BDD elseBDD) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BDD relProd(BDD other, BDD var) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BDD compose(BDD other, int var) {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		return 0;
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
		return high;
	}

	@Override
	public BDD low() {
		BDD fullBDD = ger.getFullBDD();
		BDD low = fullBDD.low();
		fullBDD.free();
		return low;
	}

	@Override
	public BDD squeezeEquiv(LeaderFunction leaderFunction) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BDD squeezeEquivWith(LeaderFunction leaderFunction) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Factory getFactory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString() {
		return ger.getEquiv() + System.lineSeparator() + ger.getN();
	}
}
