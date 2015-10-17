/**
* Find bad characters in database: perl -ne 'print "$. $_" if m/[\x80-\xFF]/' coca/basewrd3.txt 
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
		println "Number of words in input: ${inputSet.size()}"

		def inputKBlocks = [:]
		cocaSetMap.each { key, value ->
			inputKBlocks[key] = inputSet.intersect(value)
			println "Number of k-${key} words: ${inputKBlocks[key].size()}"
		}

		def unknownSet = inputSet
		inputKBlocks.each { key, value ->
			unknownSet = unknownSet.minus(inputKBlocks[key])
		}
		// Set unknownSet = inputSet.minus(k1Words).minus(k2Words).minus(k3Words).minus(k4Words).minus(k5Words)
		println "Number of word not in database: ${unknownSet.size()}"

	}

	Set individualWords(String text) {
		String[] words = text.split(/[^a-zA-Z]/) as List
		return words.collect{ (it as String).toLowerCase() }.findAll{ it != '' }.sort().unique() as Set
	}

}