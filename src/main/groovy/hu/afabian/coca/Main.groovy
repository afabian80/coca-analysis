/**
* Find bad characters in database: perl -ne 'print "$. $_" if m/[\x80-\xFF]/' coca/basewrd3.txt
* Sublime: search for [^\x00-\x7F] regex
**/

package hu.afabian.coca

import java.util.Set
import groovy.io.FileType
import java.util.regex.Matcher
import groovyx.gpars.extra166y.Ops


class Main {
	private File inputFile
	private Map cocaSetMap
	private String outputDirName
	private Map inputKiloBlocks
	private Set inputSet
	private int inputSize
	private Set unknownSet
	private Double percentageSoFar
	private File outputDir
	private int greenBlockStart
	private int redBlockStart

	Main(inputFileName, greenStart, redStart) {
		this.inputFile = new File(inputFileName)
		this.greenBlockStart = greenStart
		this.redBlockStart = redStart
		this.cocaSetMap = [:]
		this.inputKiloBlocks = [:]
		this.outputDirName = 'coca-analysis-output'
		this.percentageSoFar = 0.0
		outputDir = new File(this.outputDirName)
	}

	static void main(String[] args) {
		def start = System.currentTimeMillis()

		if(args.length != 3) {
			println('Usage: <executable> subtitle.srt <green-block-start> <red-block-start>')
			throw new RuntimeException('Missing arguments!')
		}
		String inputFileName = args[0]
		int greenBlockStart = args[1] as Integer
		int redBlockStart = args[2] as Integer

		Main main = new Main(inputFileName, greenBlockStart, redBlockStart)
		main.loadCocaDatabase()
		main.collectInputWords()
		main.partitionInputSet()
		//main.saveAnalysisOutput()
		main.saveColorizedInput()

		def end = System.currentTimeMillis()
		def duration = end - start
		println "Execution took $duration ms"
	}

	void loadCocaDatabase() {
		File cocaDir = new File(this.getClass().getResource('/cocadb').getFile())

		print "Processing coca files "
		cocaDir.eachFileRecurse(FileType.FILES) { cocaFile ->
			def matcher = (cocaFile.name =~ "basewrd(.*).txt")
			String actualSet = matcher[0][1]
			print "."
			this.cocaSetMap[actualSet] = individualWords(cocaFile.text)
		}
		println " done."
	}

	void collectInputWords() {
		println "Collecting words from input file..."
		this.inputSet = individualWords(this.inputFile.text)
		this.inputSize = inputSet.size()
		println "Number of words in input: ${this.inputSize}"
	}

	Set individualWords(String text) {
		groovyx.gpars.GParsPool.withPool {
			String[] words = text.split(/[^a-zA-Z]/) as Set
			def collected = words.collectParallel { (it as String).toLowerCase() }
			def sorted = collected.sort()
			return collected
		}
	}

	void partitionInputSet() {
		this.cocaSetMap.each { key, value ->
			registerInputBlock(key, value)
		}
		registerUnknownBlock()
	}

	void registerInputBlock(key, value) {
		def block = this.inputSet.intersect(value)
		this.inputKiloBlocks[key] = block
		printBlockStats(block, key)
	}

	void registerUnknownBlock() {
		this.unknownSet = this.inputSet
		this.inputKiloBlocks.each { key, value ->
			this.unknownSet = this.unknownSet.minus(this.inputKiloBlocks[key])
		}
		printBlockStats(this.unknownSet, '??')
	}

	void printBlockStats(block, key) {
		Integer blockSize = block.size()
		String blockSizeText = sprintf('%8d', blockSize)

		Double percentage = 100.0 * (double) blockSize / (this.inputSize)
		String percentageText = sprintf('%6.2f', percentage)

		this.percentageSoFar += percentage
		String percentageSoFarText = sprintf('%6.2f', percentageSoFar)

		println "Number of k-${key} words: $blockSizeText ($percentageText %  -> $percentageSoFarText %)"
	}

	void saveAnalysisOutput() {
		println "Saving words to '${this.outputDir}' directory..."
		ensureCleanOutputDir(outputDir)

		this.inputKiloBlocks.each { key, value ->
			writeBlockToFile(key, value)
		}
		writeBlockToFile('unknown', unknownSet)
	}

	void ensureCleanOutputDir(dir) {
		if(outputDir.exists()) {
			outputDir.deleteDir()
		}
		outputDir.mkdirs()
	}

	void writeBlockToFile(key, value) {
		def blockFile = new File(outputDir, "k-${key}.txt")
		blockFile.text = value.join('\n')
	}

	void saveColorizedInput() {
		println "Saving colorized input to '${this.outputDir}' directory..."
		// WARN: this deleted the output
		ensureCleanOutputDir(outputDir)
		def colorizedOutput = new File(outputDir, "colorized.html")
		colorizedOutput.text = colorize(inputFile.text)
	}

	String colorize(text) {
		def resultLines = []
		Set greenWords = combineGreenWords().minus([""] as Set)
		def greenSize = greenWords.size()
		println "\nFound $greenSize green words ($greenWords)\n"
		Set redWords = combineRedWords().minus([""] as Set)
		def redSize = redWords.size()
		println "\nFound $redSize red words ($redWords)\n"

		resultLines << "<html><body>"
		
		text.eachLine { line, index ->
			def lineWords = line.split(/[^a-zA-Z]/)
			def colorizedLine = line
			lineWords.each { lineWord ->
				if(lineWord.toLowerCase() in greenWords) {
					colorizedLine = colorizedLine.replace(lineWord, "<span style=\"background-color:LightGreen\">${lineWord}</span>")
				}
				if(lineWord.toLowerCase() in redWords) {
					colorizedLine = colorizedLine.replace(lineWord, "<span style=\"background-color:DarkSalmon\">${lineWord}</span>")
				}
			}
			resultLines << "<p>${colorizedLine}</p>"
		}
		resultLines << "</body></html>"
		return resultLines.join('\n')
	}

	Set combineGreenWords() {
		int from = greenBlockStart
		int to = redBlockStart - 1
		println "Combining green words from $from to $to"
		return combineWords(from, to)
	}

	Set combineRedWords() {
		int from = redBlockStart
		int to = 30
		println "Combining red words from $from to $to"
		Set redWords = combineWords(from, to)
		return redWords.plus(this.unknownSet)
	}

	Set combineWords(int from, int to=0) {
		def combinedSet = [] as Set
		(from..to).each { num ->
			def numText = sprintf('%02d', num)
			combinedSet += inputKiloBlocks[numText]
		}
		return combinedSet
	}

}