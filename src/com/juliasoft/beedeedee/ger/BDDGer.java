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

	public BDDGer(BDD bdd) {
		if (bdd != null) {
			ger = new GER(bdd).normalize();
		}
	}

	private BDDGer(GER ger) {
		this.ger = ger;
	}

	@Override
	public void free() {
		ger.getN().free();
	}

	@Override
	public boolean isZero() {
		return ger.getFullBDD().isZero();
	}

	@Override
	public boolean isOne() {
		return ger.getFullBDD().isOne();
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
		otherBddGer.ger.getN().free();
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
		otherBddGer.ger.getN().free();
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
		otherBddGer.ger.getN().free();
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

		return new BDDGer(andGer.not());
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
		otherBddGer.ger.getN().free();
		ger = andGer.not();

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
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		return 0;
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
		return ger.getFullBDD().isEquivalentTo(other);
	}

	@Override
	public int hashCodeAux() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int var() {
		return ger.getFullBDD().var();
	}

	@Override
	public BDD high() {
		return ger.getFullBDD().high();
	}

	@Override
	public BDD low() {
		return ger.getFullBDD().low();
	}

	@Override
	public BDD squeezeEquiv(LeaderFunction leaderFunction) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Factory getFactory() {
		// TODO Auto-generated method stub
		return null;
	}

}
