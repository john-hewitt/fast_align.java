package edu.upenn.cis;

import static org.junit.Assert.*;

import org.junit.Test;


public class DiagonalAlignmentTest {
	double lambda = .1;
	int n = 5;
	int m = 6;
	int i = 3;
	int j = 4;

	@Test
	public void testUnormalizedProbCorrectness() {
		assertEquals(0.9704455335485082, DiagonalAlignment.unnormalizedProb(i,j,m,n, lambda), .0001);
	}

	@Test
	public void testComputeZCorrectness() {
		int jUp = Math.floorDiv(i*n, m);
		int jDown = jUp - 1;
		double g1_jUp = 0.9900498337491681;
		double g1_jDown = 0.9900498337491681;
		double r = 0.9801986733067553;
		double s_jUp = 1.9604953672976786;
		double s_jDown = 2.911724791798388;
		double zPartition = s_jUp + s_jDown;
		assertEquals(zPartition,DiagonalAlignment.computeZ(i, m, n, lambda), 0.001);
	}

	@Test
	public void testComputeDLogZCorrectness() {
		fail("Not yet implemented");
	}
	
	@Test
	public void testFeatureCorrectness() {
		assertEquals( -0.3, DiagonalAlignment.feature(i,j,m,n), .0001);
	}
	
	@Test
	public void testArithmeticoGemoetricSeriesCorrectness() {
//		double a_1 = -0.09999999999999998;
//		double gJdown_lplus1 = 0.9512294245007139;
//		double gJdown_1 = 0.9900498337491681;
//		double gJup_1 = 0.9900498337491681;
//		double gJup_lplus1 = 0.9323938199059482;
//		double aJup_l = -0.13999999999999999;
//		double aJdown_l = -0.13999999999999999;
//		double r = 0.9801986733067553;
//		double d = -1/n;
//		double expected = (aJup_l*gJup_lplus1)/(1- r) + d * (gJup_lplus1 - gJup_1 * r)/Math.pow((1-r), 2);
		double r = 0.9801986733067553;
		double d = -1/n;
		double a_1 = DiagonalAlignment.feature(i, 2, m, n);
		double g_1 = Math.exp(DiagonalAlignment.feature(i, 2, m, n) * lambda);
		double g_l_plus_1 = g_1 * Math.pow(r, 2);
		double a_l = a_1 + d * (1);
		double expected = (a_l * g_l_plus_1 - a_1 * g_1) / (1 - r) + d*(g_l_plus_1 - g_1 * r)/(Math.pow(1 - r, 2));
		assertEquals(expected, DiagonalAlignment.arithmeticoGeometricSeries(a_1, g_1, r, d, 2 ), 0.0001);
	} 
}
