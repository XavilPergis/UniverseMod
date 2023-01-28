package net.xavil.universal.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;

public class NameTemplate {

	public static final NameTemplate GALAXY_NAME = NameTemplate.compile("[(<M>d?d?d)(<NGC >dddddd?d?d)]");
	public static final NameTemplate SECTOR_NAME = NameTemplate.compile("[(BV)(VCV)]?(CV)?(CL)< >^*^*<->^*");

	public static class PatternTable {
		private final Map<Integer, List<String>> templateMap = new HashMap<>();

		public static final PatternTable DEFAULT = new PatternTable();

		static {
			DEFAULT.addMappings('d', "0", "1", "2", "3", "4", "5", "6", "7", "8", "9");

			DEFAULT.addMappings('v', "a", "e", "i", "o", "u", "y");

			DEFAULT.addMappings('V', "a", "e", "i", "o", "u", "y", "ae", "ai", "au", "ay", "ea", "ee", "ei", "eu", "ey",
					"ia", "ie", "oe", "oi", "oo", "ou", "ui");

			DEFAULT.addMappings('c', "b", "c", "d", "f", "g", "h", "j", "k", "l", "m", "n", "p", "q", "r", "s", "t",
					"v", "w", "x", "y", "z");

			DEFAULT.addMappings('*', "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p",
					"q", "r", "s", "t", "u", "v", "w", "x", "y", "z");

			DEFAULT.addMappings('B', "b", "bl", "br", "c", "ch", "chr", "cl", "cr", "d", "dr", "f", "g", "h", "j", "k",
					"l", "ll", "m", "n", "p", "ph", "qu", "r", "rh", "s", "sch", "sh", "sl", "sm", "sn", "st", "str",
					"sw", "t", "th", "thr", "tr", "v", "w", "wh", "y", "z", "zh");

			DEFAULT.addMappings('C', "b", "c", "ch", "ck", "d", "f", "g", "gh", "h", "k", "l", "ld", "ll", "lt", "m",
					"n", "nd", "nn", "nt", "p", "ph", "q", "r", "rd", "rr", "rt", "s", "sh", "ss", "st", "t", "th", "v",
					"w", "y", "z");

			DEFAULT.addMappings('S', "cent", "elys", "had", "olymp", "amic", "aquil", "virg", "cet", "ke", "andr");
			DEFAULT.addMappings('L', "a", "ae", "am", "i", "o", "um", "us", "is");

			// aquilae sector
			// iman caber
		}

		private void addMappings(char ch, String... expansions) {
			if (!this.templateMap.containsKey((int) ch)) {
				this.templateMap.put((int) ch, new ArrayList<>());
			}
			this.templateMap.get((int) ch).addAll(Arrays.asList(expansions));
		}
	}

	private static class ParsingContext {
		public final PatternTable table;
		public final String input;
		public int cursor = 0;

		public ParsingContext(PatternTable table, String input) {
			this.table = table;
			this.input = input;
		}

		public boolean hasNext() {
			return this.input.length() > this.cursor;
		}

		public int peekNext() {
			return this.input.codePointAt(this.cursor);
		}

		public int advanceNext() {
			var codePoint = this.input.codePointAt(this.cursor);
			this.cursor += Character.charCount(codePoint);
			return codePoint;
		}
	}

	private static boolean expectSequenceEnd(ParsingContext ctx, int sequenceStart, int sequenceEnd) {
		if (!ctx.hasNext()) {
			throw new RuntimeException("mistmatched '" + Character.toString(sequenceStart) + "': unterminated opener");
		}

		var next = ctx.peekNext();
		if (next == sequenceEnd) {
			ctx.advanceNext();
			return true;
		}

		if (next == ')' || next == ']' || next == '>') {
			throw new RuntimeException(
					"mistmatched '" + Character.toString(sequenceStart) + "': expected '"
							+ Character.toString(sequenceEnd) + "', got '"
							+ Character.toString(next) + "'");
		}

		return false;
	}

	public static Node compileTerm(ParsingContext ctx) {
		while (ctx.hasNext() && Character.isWhitespace(ctx.peekNext())) {
			ctx.advanceNext();
		}

		if (!ctx.hasNext())
			return new Node.Sequence();

		var ch = ctx.advanceNext();
		switch (ch) {
			// group
			case '(' -> {
				var node = new Node.Sequence();
				while (!expectSequenceEnd(ctx, '(', ')')) {
					node.childNodes.add(compileTerm(ctx));
				}
				return node;
			}
			// literal
			case '<' -> {
				var literal = "";
				while (!expectSequenceEnd(ctx, '<', '>')) {
					if (ctx.peekNext() == '\\') {
						ctx.advanceNext();
						if (!ctx.hasNext()) {
							throw new RuntimeException("unterminated escape sequence");
						}
						var escapeCode = ctx.advanceNext();
						var escaped = switch (escapeCode) {
							case '>' -> '>';
							default -> throw new RuntimeException(
									"unknown escape sequence '" + Character.toString(escapeCode) + "'");
						};
						literal += Character.toString(escaped);
					} else {
						literal += Character.toString(ctx.advanceNext());
					}
				}
				return new Node.Literal(literal);
			}
			// choice
			case '[' -> {
				var node = new Node.Choice();
				while (!expectSequenceEnd(ctx, '[', ']')) {
					node.childNodes.add(compileTerm(ctx));
				}
				return node;
			}
			// optional
			case '?' -> {
				if (!ctx.hasNext()) {
					throw new RuntimeException("unterminated optional");
				}
				return new Node.Optional(compileTerm(ctx));
			}
			// capitalize
			case '^' -> {
				if (!ctx.hasNext()) {
					throw new RuntimeException("unterminated capitalization");
				}
				return new Node.Capitalize(compileTerm(ctx));
			}
			// index
			default -> {
				return new Node.Lookup(ch);
			}
		}
	}

	public static Node.Sequence compileRoot(ParsingContext ctx) {
		var node = new Node.Sequence();
		while (ctx.hasNext()) {
			node.childNodes.add(compileTerm(ctx));
		}
		return node;
	}

	public static NameTemplate compile(String template) {
		return compile(PatternTable.DEFAULT, template);
	}

	public static NameTemplate compile(PatternTable table, String template) {
		var ctx = new ParsingContext(table, template);
		var node = compileRoot(ctx).optimize();
		return new NameTemplate(table, node);
	}

	public String generate(Random random) {
		var builder = new StringBuilder();
		var ctx = new Node.EvalContext(random, this.table);
		this.node.evaluate(ctx, builder);
		return builder.toString();
	}

	private final PatternTable table;
	private final Node node;

	private NameTemplate(PatternTable table, Node node) {
		this.table = table;
		this.node = node;
	}

	public static abstract sealed class Node {

		public record EvalContext(Random random, PatternTable table) {
		}

		public static final class Literal extends Node {

			public final String value;

			public Literal(String value) {
				this.value = value;
			}

			@Override
			public void evaluate(EvalContext ctx, StringBuilder builder) {
				builder.append(this.value);
			}

			public Node optimize() {
				return this;
			};
		}

		public static final class Lookup extends Node {
			public final int value;

			public Lookup(int value) {
				this.value = value;
			}

			@Override
			public void evaluate(EvalContext ctx, StringBuilder builder) {
				var stems = ctx.table.templateMap.get(this.value);
				builder.append(stems.get(ctx.random.nextInt(stems.size())));
			}

			public Node optimize() {
				return this;
			};
		}

		public static final class Optional extends Node {
			public final Node childNode;

			public Optional(Node childNode) {
				this.childNode = childNode;
			}

			@Override
			public void evaluate(EvalContext ctx, StringBuilder builder) {
				if (ctx.random.nextBoolean())
					this.childNode.evaluate(ctx, builder);
			}

			@Override
			public Node optimize() {
				return new Optional(this.childNode.optimize());
			}

		}

		public static final class Choice extends Node {
			public final List<Node> childNodes = new ArrayList<>();

			@Override
			public void evaluate(EvalContext ctx, StringBuilder builder) {
				if (this.childNodes.isEmpty())
					return;
				this.childNodes.get(ctx.random.nextInt(this.childNodes.size())).evaluate(ctx, builder);
			}

			public Node optimize() {
				if (this.childNodes.size() == 1) {
					return this.childNodes.get(0).optimize();
				}
				var optimizedChildren = this.childNodes.stream().map(Node::optimize).toList();
				var childChoiceCount = -1;
				var canMerge = true;
				for (var optimizedChild : optimizedChildren) {
					if (!canMerge)
						break;
					if (optimizedChild instanceof Choice choice) {
						if (childChoiceCount == -1) {
							childChoiceCount = choice.childNodes.size();
						} else {
							canMerge = childChoiceCount == choice.childNodes.size();
						}
					} else {
						canMerge = false;
					}
				}

				var newNode = new Choice();
				if (canMerge) {
					for (var optimizedChild : optimizedChildren) {
						// if canMerge is true, then we know that all the children of this choice node
						// are also choice nodes.
						var choice = (Choice) optimizedChild;
						newNode.childNodes.addAll(choice.childNodes);
					}
				} else {
					newNode.childNodes.addAll(optimizedChildren);
				}
				return newNode;
			};
		}

		public static final class Sequence extends Node {
			public final List<Node> childNodes = new ArrayList<>();

			@Override
			public void evaluate(EvalContext ctx, StringBuilder builder) {
				childNodes.forEach(child -> child.evaluate(ctx, builder));
			}

			public Node optimize() {
				if (this.childNodes.size() == 1) {
					return this.childNodes.get(0).optimize();
				}

				var newNode = new Sequence();
				for (var child : this.childNodes) {
					var optimizedChild = child.optimize();
					if (optimizedChild instanceof Sequence sequence) {
						newNode.childNodes.addAll(sequence.childNodes);
					} else {
						newNode.childNodes.add(optimizedChild);
					}
				}
				return newNode;
			};
		}

		public static final class Capitalize extends Node {
			public final Node childNode;

			public Capitalize(Node childNode) {
				this.childNode = childNode;
			}

			@Override
			public void evaluate(EvalContext ctx, StringBuilder builder) {
				var childBuilder = new StringBuilder();
				this.childNode.evaluate(ctx, childBuilder);
				builder.append(StringUtils.capitalize(childBuilder.toString()));
			}

			public Node optimize() {
				return new Capitalize(this.childNode.optimize());
			};
		}

		public abstract void evaluate(EvalContext ctx, StringBuilder builder);

		public abstract Node optimize();
	}

}
