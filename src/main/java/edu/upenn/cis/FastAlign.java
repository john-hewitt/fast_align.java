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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import java.lang.Integer;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

public class FastAlign {

	private static class Pair {
		private final int first;
		private final int second;

		private Pair(int first, int second) {
			this.first = first;
			this.second = second;
		}

		public boolean equals(Object o) {
			if (o instanceof Pair) {
				Pair other = (Pair) o;
				return this.first==other.first && this.second==other.second;
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return first << 16 | second;
		}
	}

	private final Dict d; // integerization map

	private final String input;
	private final String conditional_probability_filename;
	private final String existing_probability_filename;
	private final boolean is_reverse;
	private final int iterations;
	private final boolean favor_diagonal;
	private final double prob_align_null;
	private double diagonal_tension;
	private final boolean optimize_tension;
	private final boolean variational_bayes;
	private final double alpha;
	private final boolean no_null_word;	

	private FastAlign(String input,
			String conditional_probability_filename,
			String existing_probability_filename,
			boolean is_reverse,
			int iterations,
			boolean favor_diagonal,
			double prob_align_null,
			double diagonal_tension,
			boolean optimize_tension,
			boolean variational_bayes,
			double alpha,
			boolean no_null_word) {
		this.input = input;
		this.conditional_probability_filename = conditional_probability_filename;
		this.existing_probability_filename = existing_probability_filename;
		this.is_reverse = is_reverse;
		this.iterations = iterations;
		this.favor_diagonal = favor_diagonal;
		this.prob_align_null = prob_align_null;
		this.diagonal_tension = diagonal_tension;
		this.optimize_tension = optimize_tension;
		this.variational_bayes = variational_bayes;
		this.alpha = alpha;
		this.no_null_word = no_null_word;
		this.d = new Dict();
	}

	/**
	 * Populates source and target token lists with integerized tokens.
	 * 
	 * @param line  a line of the bitext
	 * @param src  the list to be populated with source tokens
	 * @param tgt  the list to be populated with target tokens
	 */
	private void ParseLine(final String line,
			ArrayList<Integer> src,
			ArrayList<Integer> trg) {
		final int kDIV = d.Convert("|||");
		ArrayList<Integer> tmp = new ArrayList<Integer>();
		src.clear();
		trg.clear();
		d.ConvertWhitespaceDelimitedLine(line, tmp);
		int i = 0;
		while(i < tmp.size() && tmp.get(i) != kDIV) {
			src.add(tmp.get(i));
			++i;
		}
		if (i < tmp.size() && tmp.get(i) == kDIV) {
			++i;
			for (; i < tmp.size() ; ++i)
				trg.add(tmp.get(i));
		}
	}

	public static FastAlign initCommandLine(String[] argv) {
		String input = "";
		String conditional_probability_filename = "";
		String existing_probability_filename = "";
		boolean is_reverse = false;
		int iterations = 5;
		boolean favor_diagonal = false;
		double prob_align_null = 0.08;
		double diagonal_tension = 4.0;
		boolean optimize_tension = false;
		boolean variational_bayes = false;
		double alpha = 0.01;
		boolean no_null_word = false;
		
		Options ops = new Options();
		ops.addOption(Option.builder("i")
				.hasArg()
				.desc("[REQ] Input parallel corpus")
				.required()
				.build());
		ops.addOption("v", false, "[USE] Use Dirichlet prior on lexical translation distributions");
		ops.addOption("d", false, "[USE] Favor alignment points close to the monotonic diagonal");
		ops.addOption("o", false, "[USE] Optimize how close to the diagonal alignment points should be.");
		ops.addOption("r", false, "Run alignment in reverse (condition on target and predict source");
		ops.addOption("c", false, "Output conditional probability table");
		ops.addOption("e", true, "Start with existing conditional probability table");
		ops.addOption("I", true, "number of iterations in EM training (default = 5)");
		ops.addOption("p", true, "p_null parameter");
//		ops.addOption( Option.builder("p")
//                .desc( "use SIZE-byte blocks" )
//                .hasArg()
//                .type(Integer.class)
//                .build() );
		ops.addOption("N", false, "No null word");
		ops.addOption("a", true, "alpha parameter for optional Dirichlet prior (default = 0.01");
		ops.addOption("T", true, "starting lambda for diagonal distance parameter");
		
		CommandLineParser parser = new DefaultParser();
		CommandLine line = null;
		try {
			line = parser.parse(ops, argv);
		} catch (ParseException exp) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "java -jar fast_align-1.0_SNAPSHOT.jar -i [PARALLEL_CORPUS] [OPTIONS]", ops );
			System.err.println( "Invalid command line arguments: " + exp.getMessage() );
			System.exit(1);
		}
		if (line.hasOption("i")) {
			input = line.getOptionValue("i");
		}
		if (line.hasOption("v")) {
			variational_bayes = true;
		}
		if (line.hasOption("d")) {
			favor_diagonal = true;
		}
		if (line.hasOption("p")) {
			prob_align_null = Double.valueOf(line.getOptionValue("p"));
		}
		if (line.hasOption("T")) {
			diagonal_tension = Double.valueOf(line.getOptionValue("T"));
		}
		if (line.hasOption("o")) {
			optimize_tension = true;
		}
		if (line.hasOption("v")) {
			variational_bayes = true;
		}
		if (line.hasOption("a")) {
			alpha = Double.valueOf(line.getOptionValue("a"));
		}
		if (line.hasOption("N")) {
			no_null_word = true;
		}
		if (line.hasOption("c")) {
			conditional_probability_filename = line.getOptionValue("c");
		}
		if (line.hasOption("e")) {
			existing_probability_filename = line.getOptionValue("e");
		}
		
		return new FastAlign(
				input,
				conditional_probability_filename,
				existing_probability_filename,
				is_reverse,
				iterations,
				favor_diagonal,
				prob_align_null,
				diagonal_tension,
				optimize_tension,
				variational_bayes,
				alpha,
				no_null_word);
	}
	
	
	/**
	 * Prints alignments for options specified by command line arguments.
	 * @param argv  parameters to be used by FastAlign.
	 */
	public static void main(String[] argv) {

		FastAlign align = FastAlign.initCommandLine(argv);
		if (align==null) {
			System.err.println(
					"Usage: java " + FastAlign.class.getCanonicalName() + " -i file.fr-en\n"
							+ " Standard options ([USE] = strongly recommended):\n"
							+ "  -i: [REQ] Input parallel corpus\n"
							+ "  -v: [USE] Use Dirichlet prior on lexical translation distributions\n"
							+ "  -d: [USE] Favor alignment points close to the monotonic diagonoal\n"
							+ "  -o: [USE] Optimize how close to the diagonal alignment points should be\n"
							+ "  -r: Run alignment in reverse (condition on target and predict source)\n"
							+ "  -c: Output conditional probability table\n"
							+ "  -e: Start with existing conditional probability table\n"
							+ " Advanced options:\n"
							+ "  -I: number of iterations in EM training (default = 5)\n"
							+ "  -p: p_null parameter (default = 0.08)\n"
							+ "  -N: No null word\n"
							+ "  -a: alpha parameter for optional Dirichlet prior (default = 0.01)\n"
							+ "  -T: starting lambda for diagonal distance parameter (default = 4)\n"
					);
			System.exit(1);
		}
		boolean use_null = !align.no_null_word;
		if (align.variational_bayes && align.alpha <= 0.0) {
			System.err.println("--alpha must be > 0\n");
			System.exit(1);
		}
		double prob_align_not_null = 1.0 - align.prob_align_null;
		final int kNULL = align.d.Convert("<eps>");
		TTable s2t = new TTable();
		if (!align.existing_probability_filename.isEmpty()) {
			boolean success = s2t.ImportFromFile(align.existing_probability_filename, '\t', align.d);
			if (!success) {
				System.err.println("Can't read table " + align.existing_probability_filename);
				System.exit(1);
			}
		}
		Map<Pair, Integer> size_counts = new HashMap<Pair, Integer>();
		double tot_len_ratio = 0;
		double mean_srclen_multiplier = 0;
		List<Double> probs = new ArrayList<Double>();;
		// E-M Iterations Loop TODO move this into a method?
		for (int iter = 0; iter < align.iterations || (iter==0 && align.iterations==0); ++iter) {
			final boolean final_iteration = (iter >= (align.iterations - 1));
			System.err.println("ITERATION " + (iter + 1) + (final_iteration ? " (FINAL)" : ""));
			Scanner in = null;
			try {
				in = new Scanner(new File(align.input));
				if (! in.hasNextLine()) {
					System.err.println("Can't read " + align.input);
					System.exit(1);
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.err.println("Can't read " + align.input);
				System.exit(1);
			}

			double likelihood = 0;
			double denom = 0.0;
			int lc = 0;
			boolean flag = false;
			String line;
//			String ssrc, strg;
			ArrayList<Integer> src = new ArrayList<Integer>();
			ArrayList<Integer> trg = new ArrayList<Integer>();
			double c0 = 0;
			double emp_feat = 0;
			double toks = 0;
			// Iterate over each line of the input file
			while (in.hasNextLine()) {
				line = in.nextLine();
				++lc;
				if (lc % 1000 == 0) { System.err.print('.'); flag = true; }
				if (lc %50000 == 0) { System.err.println(" [" + lc + "]\n"); System.err.flush(); flag = false; }
				src.clear(); trg.clear(); // TODO this is redundant; src and tgt cleared in ParseLine
				// Integerize and split source and target lines.
				align.ParseLine(line, src, trg);
				if (align.is_reverse) {
					ArrayList<Integer> tmp = src;
					src = trg;
					trg = tmp;
				}
				// TODO Empty lines break the parser. Should this be true?
				if (src.size() == 0 || trg.size() == 0) {
					System.err.println("Error in line " + lc + "\n" + line);
					System.exit(1);
				}
				if (iter == 0) {
					tot_len_ratio += ((double) trg.size()) / ((double) src.size());
				}
				denom += trg.size();
				probs.clear();
				// Add to pair length counts only if first iteration.
				if (iter == 0) {
					Pair pair = new Pair(trg.size(), src.size());
					Integer value = size_counts.get(pair);
					if (value==null) value=0;
					size_counts.put(pair, value+1);
				}
				boolean first_al = true;  // used when printing alignments
				toks += trg.size();
				// Iterate through the English tokens
				for (int j = 0; j < trg.size(); ++j) {
					final int f_j = trg.get(j);
					double sum = 0;
					double prob_a_i = 1.0 / (src.size() + (use_null ? 1 : 0));  // uniform (model 1)
					if (use_null) {
						if (align.favor_diagonal) {
							prob_a_i = align.prob_align_null;
						}
						probs.add(0, s2t.prob(kNULL, f_j) * prob_a_i);
						sum += probs.get(0);
					}
					double az = 0;
					if (align.favor_diagonal)
						az = DiagonalAlignment.computeZ(j+1, trg.size(), src.size(), align.diagonal_tension) / prob_align_not_null;
					for (int i = 1; i <= src.size(); ++i) {
						if (align.favor_diagonal)
							prob_a_i = DiagonalAlignment.unnormalizedProb(j + 1, i, trg.size(), src.size(), align.diagonal_tension) / az;
						probs.add(i, s2t.prob(src.get(i-1), f_j) * prob_a_i);
						sum += probs.get(i);
					}
					if (final_iteration) {
						double max_p = -1;
						int max_index = -1;
						if (use_null) {
							max_index = 0;
							max_p = probs.get(0);
						}
						for (int i = 1; i <= src.size(); ++i) {
							if (probs.get(i) > max_p) {
								max_index = i;
								max_p = probs.get(i);
							}
						}
						if (max_index > 0) {
							if (first_al) first_al = false; else System.out.print(' ');
							if (align.is_reverse)
								System.out.print("" + j + '-' + (max_index - 1));
							else
								System.out.print("" + (max_index - 1) + '-' + j);
						}
					} else {
						if (use_null) {
							double count = probs.get(0) / sum;
							c0 += count;
							s2t.Increment(kNULL, f_j, count);
						}
						for (int i = 1; i <= src.size(); ++i) {
							final double p = probs.get(i) / sum;
							s2t.Increment(src.get(i-1), f_j, p);
							emp_feat += DiagonalAlignment.feature(j, i, trg.size(), src.size()) * p;
						}
					}
					likelihood += Math.log(sum);
				}
				if (final_iteration) System.out.println();
			}

			// log(e) = 1.0
			double base2_likelihood = likelihood / Math.log(2);

			if (flag) { System.err.println(); }
			if (iter == 0) {
				mean_srclen_multiplier = tot_len_ratio / lc;
				System.err.println("expected target length = source length * " + mean_srclen_multiplier );
			}
			emp_feat /= toks;
			System.err.println("  log_e likelihood: " + likelihood );
			System.err.println("  log_2 likelihood: " + base2_likelihood );
			System.err.println("     cross entropy: " + (-base2_likelihood / denom) );
			System.err.println("        perplexity: " + Math.pow(2.0, -base2_likelihood / denom) );
			System.err.println("      posterior p0: " + c0 / toks );
			System.err.println(" posterior al-feat: " + emp_feat );
			//System.err.println("     model tension: " + mod_feat / toks );
			System.err.println("       size counts: " + size_counts.size() );
			if (!final_iteration) {
				if (align.favor_diagonal && align.optimize_tension && iter > 0) {
					for (int ii = 0; ii < 8; ++ii) {
						double mod_feat = 0;
						Iterator<Map.Entry<Pair,Integer>> it = size_counts.entrySet().iterator();
						for(; it.hasNext(); ) {
							Map.Entry<Pair,Integer> entry = it.next();
							final Pair p = entry.getKey();
							for (int j = 1; j <= p.first; ++j)
								mod_feat += entry.getValue() * DiagonalAlignment.computeDLogZ(j, p.first, p.second, align.diagonal_tension);
						}
						mod_feat /= toks;
						System.err.println("  " + ii + 1 + "  model al-feat: " + mod_feat + " (tension=" + align.diagonal_tension + ")");
						align.diagonal_tension += (emp_feat - mod_feat) * 20.0;
						if (align.diagonal_tension <= 0.1) align.diagonal_tension = 0.1;
						if (align.diagonal_tension > 14) align.diagonal_tension = 14;
					}
					System.err.println("     final tension: " + align.diagonal_tension);
				}
				if (align.variational_bayes)
					s2t.NormalizeVB(align.alpha);
				else
					s2t.Normalize();
				//prob_align_null *= 0.8; // XXX
				//prob_align_null += (c0 / toks) * 0.2;
				prob_align_not_null = 1.0 - align.prob_align_null;
			}

		}
		if (!align.conditional_probability_filename.isEmpty()) {
			System.err.println("conditional probabilities: " + align.conditional_probability_filename);
			s2t.ExportToFile(align.conditional_probability_filename, align.d);
		}
		System.exit(0);
	}
}
