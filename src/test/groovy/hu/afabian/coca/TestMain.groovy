package hu.afabian.coca

import spock.lang.Specification

class TestMain extends Specification {

	def "valami"() {
		given:
			def main = new Main('cica', 1, 2)
		when:
			def res = main.combineGreenWords()
		then:
			res == null
	}
}