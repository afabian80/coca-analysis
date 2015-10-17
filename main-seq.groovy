/**
* Find bad characters in database: perl -ne 'print "$. $_" if m/[\x80-\xFF]/' coca/basewrd3.txt 
* Sublime: search for [^\x00-\x7F] regex
**/

import java.util.Set
import groovy.io.FileType
import java.util.regex.Matcher

class Main {
	static void main(String[] args) {

		if(args.length != 1) {
			println('Usage: <executable> subtitle.srt')
			throw new RuntimeException('Missing arguments!')
		}

		Main main = new Main()
		File cocaDir = new File('coca')
		def cocaSetMap = [:]
		def outputDirName = 'output'

		cocaDir.eachFileRecurse(FileType.FILES) { cocaFile ->
			def matcher = (cocaFile.name =~ "basewrd(.*).txt")
			String actualSet = matcher[0][1]
			println "Processing coca ${actualSet}-k database..."
			cocaSetMap[actualSet] = main.individualWords(cocaFile.text)
		}

		File file = new File(args[0])
		String inputText = file.text

		println "Collecting words from input file..."

		Set inputSet = main.individualWords(inputText)
		Integer inputWords = inputSet.size()
		println "Number of words in input: $inputWords"

		Double percentageSoFar = 0.0

		def inputKBlocks = [:]
		cocaSetMap.each { key, value ->
			inputKBlocks[key] = inputSet.intersect(value)
			
			Integer blockWords = inputKBlocks[key].size()
			String blockWordsText = sprintf('%8d', blockWords)
			
			Double percentage = 100.0 * (double) blockWords / inputWords
			String percentageText = sprintf('%6.2f', percentage)

			percentageSoFar += percentage
			String percentageSoFarText = sprintf('%6.2f', percentageSoFar)
			
			println "Number of k-${key} words: $blockWordsText ($percentageText %  -> $percentageSoFarText %)"
		}

		def unknownSet = inputSet
		inputKBlocks.each { key, value ->
			unknownSet = unknownSet.minus(inputKBlocks[key])
		}
		Integer unknownWords = unknownSet.size()
		Double percentage = 100.0 * (double) unknownWords / inputWords
		String percentageText = sprintf('%6.2f', percentage)
		// Set unknownSet = inputSet.minus(k1Words).minus(k2Words).minus(k3Words).minus(k4Words).minus(k5Words)
		println "Number of word not in database: $unknownWords ($percentageText %)"

		println "Saving words to $outputDirName directory..."
		def outputDir = new File(outputDirName)
		if(outputDir.exists()) {
			outputDir.deleteDir()
		}
		outputDir.mkdirs()

		inputKBlocks.each { key, value ->
			def blockFile = new File(outputDir, "k-${key}.txt")
			blockFile.text = value.join('\n')
		}

		def unknownFile = new File(outputDir, "off-the-list.txt")
		unknownFile.text = unknownSet.join('\n')

	}

	Set individualWords(String text) {
		String[] words = text.split(/[^a-zA-Z]/) as List
		return words.collect{ (it as String).toLowerCase() }.findAll{ it != '' }.sort().unique() as Set
	}

}