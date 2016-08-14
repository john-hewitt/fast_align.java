// Copyright 2013 by Chris Dyer
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// Ported to Java by Lane Schwartz
// Modifications by John Hewitt
//
package edu.upenn.cis;

public class DiagonalAlignment {
	
	/**
	 * Computes the unnormalized probability of an (i,j) alignment conditioned on a bias-to-diagonal parameter.
	 * @param i Target index
	 * @param j Source index
	 * @param m Target length
	 * @param n Source length
	 * @param alpha bias of alignments towards a perfect diagonal.
	 * @return
	 */
	public static double UnnormalizedProb(final int i, final int j, final int m, final int n, final double alpha) {
		assert(i > 0);
		assert(n > 0);
		assert(m >= i);
		assert(n >= j);
		return Math.exp(Feature(i, j, m, n) * alpha);
	}

	/**
	 * Computes the normalizing sum of the log-partition function in O(1)
	 * @param i Target index
	 * @param j Source index
	 * @param m Target length
	 * @param n Source length
	 * @param alpha
	 * @return
	 */	
	public static double ComputeZ(final int i, final int m, final int n, final double alpha) {
		assert(i > 0);
		assert(n > 0);
		assert(m >= i);
		final double split = ((double) i) * n / m;
		final int floor = (int) Math.floor(split);
		int ceil = floor + 1;
		final double ratio = Math.exp(-alpha / n);
		final int num_top = n - floor;
		double ezt = 0;
		double ezb = 0;
		if (num_top != 0)
			ezt = UnnormalizedProb(i, ceil, m, n, alpha) * (1.0 - Math.pow(ratio, num_top)) / (1.0 - ratio);
		if (floor != 0)
			ezb = UnnormalizedProb(i, floor, m, n, alpha) * (1.0 - Math.pow(ratio, floor)) / (1.0 - ratio);
		return ezb + ezt;
	}

	/**
	 * Calculates the normalizing sum of the derivative of the log-partition function in O(1) 
	 * @param i Target index
	 * @param j Source index
	 * @param m Target length
	 * @param n Source length
	 * @param alpha
	 * @return
	 */	
	public static double ComputeDLogZ(final int i, final int m, final int n, final double alpha) {
		final double z = ComputeZ(i, m, n, alpha);
		final double split = ((double) i) * n / m;
		final int floor = (int) Math.floor(split);
		final int ceil = floor + 1;
		final double ratio = Math.exp(-alpha / n);
		final double d = -1.0 / n;
		final int num_top = n - floor;
		double pct = 0;
		double pcb = 0;
		if (num_top != 0) {
			pct = arithmetico_geometric_series(Feature(i, ceil, m, n), UnnormalizedProb(i, ceil, m, n, alpha), ratio, d, num_top);
			//cerr << "PCT = " << pct << endl;
		}
		if (floor != 0) {
			pcb = arithmetico_geometric_series(Feature(i, floor, m, n), UnnormalizedProb(i, floor, m, n, alpha), ratio, d, floor);
			//cerr << "PCB = " << pcb << endl;
		}
		return (pct + pcb) / z;
	}

	/**
	 * Calculates the h(*) value of given source and target indices based on the source
	 * and target sentence lengths. 
	 * @param i Target index
	 * @param j Source index
	 * @param m Target length
	 * @param n Source length
	 * @return 
	 */	
	public static double Feature(final int i, final int j, final int m, final int n) {
		return -Math.abs(((double) j) / n - ((double) i) / m);
	}

	/**
	 * Calculates the sum of the first n values of an arithemtico-geometric series in O(1). 
	 * @param a_1  The 0th value of the arithmetic series
	 * @param g_1  The 0th value of the geometric series
	 * @param r  The ratio of the geometric series
	 * @param d  The difference of the arithmetic series
	 * @param n  The index of the arithmetico-geometric series at which to end.
	 * @return
	 */
	private static double arithmetico_geometric_series(final double a_1, final double g_1, final double r, final double d, final int n) {
		final double g_np1 = g_1 * Math.pow(r, n);
		final double a_n = d * (n - 1) + a_1;
		final double x_1 = a_1 * g_1;
		final double g_2 = g_1 * r;
		final double rm1 = r - 1;
		return (a_n * g_np1 - x_1) / rm1 - d*(g_np1 - g_2) / (rm1 * rm1);
	}
	
}
