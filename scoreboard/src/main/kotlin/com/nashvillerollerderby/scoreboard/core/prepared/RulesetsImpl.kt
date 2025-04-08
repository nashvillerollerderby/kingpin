package com.nashvillerollerderby.scoreboard.core.prepared

import com.nashvillerollerderby.scoreboard.core.interfaces.Rulesets
import com.nashvillerollerderby.scoreboard.core.interfaces.Rulesets.Ruleset
import com.nashvillerollerderby.scoreboard.core.interfaces.ScoreBoard
import com.nashvillerollerderby.scoreboard.event.*
import com.nashvillerollerderby.scoreboard.rules.Rule
import com.nashvillerollerderby.scoreboard.rules.RuleDefinition
import com.nashvillerollerderby.scoreboard.utils.ValWithId
import java.util.*

class RulesetsImpl(s: ScoreBoard?) : ScoreBoardEventProviderImpl<Rulesets?>(s, "", ScoreBoard.RULESETS), Rulesets {
    init {
        addProperties(Rulesets.props)
        initialize()
    }

    override fun create(
        prop: Child<out ScoreBoardEventProvider?>,
        id: String,
        source: ScoreBoardEventProvider.Source
    ): ScoreBoardEventProvider? {
        if (prop === Rulesets.RULESET) {
            return RulesetImpl(this, "", null, id)
        }
        return null
    }

    override fun itemRemoved(prop: Child<*>, item: ValueWithId, source: ScoreBoardEventProvider.Source) {
        if (prop === Rulesets.RULESET) {
            // Point any rulesets with the deleted one as their parent
            // to their grandparent.
            val removed = item as Ruleset
            val grandparent = removed.parentRuleset
            for (rm in getAll(Rulesets.RULESET)) {
                if (removed == rm.parentRuleset) {
                    rm.parentRuleset = grandparent
                }
            }
        }
    }

    private fun initialize() {
        val root = RulesetImpl(this, "WFTDA", null, Rulesets.ROOT_ID)
        for (r in Rule.entries) {
            r.ruleDefinition.setParent(this)
            r.ruleDefinition.index = r.ordinal
            add(Rulesets.RULE_DEFINITION, r.ruleDefinition)
            root.setRule(r.toString(), r.ruleDefinition.defaultValue)
        }
        root.set(READONLY, true)
        add(Rulesets.RULESET, root)
        addWriteProtection(Rulesets.RULE_DEFINITION)
        addDefaultRulesets(root)
    }

    private fun addDefaultRulesets(root: Ruleset) {
        val jrda = RulesetImpl(this, "JRDA", root, "JRDARuleset")
        jrda.setRule("Jam.SuddenScoring", "true")
        jrda.setRule("Jam.InjuryContinuation", "true")
        jrda.set(READONLY, true)
        add(Rulesets.RULESET, jrda)

        val sevens = RulesetImpl(this, "Sevens", root, "SevensRuleset")
        sevens.setRule("Intermission.Durations", "60:00")
        sevens.setRule("Penalties.NumberToFoulout", "4")
        sevens.setRule("Period.Duration", "21:00")
        sevens.setRule("Period.Number", "1")
        sevens.setRule("Team.OfficialReviews", "0")
        sevens.setRule("Team.Timeouts", "0")
        sevens.set(READONLY, true)
        add(Rulesets.RULESET, sevens)

        val rdcl = RulesetImpl(this, "RDCL", root, "RDCLRuleset")
        rdcl.setRule("Intermission.Durations", "5:00,15:00,5:00,60:00")
        rdcl.setRule("Jam.Duration", "1:00")
        rdcl.setRule("Jam.ResetNumberEachPeriod", "false")
        rdcl.setRule("Penalties.DefinitionFile", "/config/penalties/RDCL.json")
        rdcl.setRule("Period.Duration", "15:00")
        rdcl.setRule("Period.EndBetweenJams", "false")
        rdcl.setRule("Period.Number", "4")
        rdcl.setRule("Team.RDCLPerHalfRules", "true")
        rdcl.setRule("Score.WftdaLateChangeRule", "false")
        rdcl.set(READONLY, true)
        add(Rulesets.RULESET, rdcl)

        val rdclHalf = RulesetImpl(this, "RDCL half game", rdcl, "RDCLHalfGameRuleset")
        rdclHalf.setRule("Intermission.Durations", "5:00,60:00")
        rdclHalf.setRule("Penalties.NumberToFoulout", "4")
        rdclHalf.setRule("Period.Number", "2")
        rdclHalf.setRule("Team.Timeouts", "1")
        rdclHalf.setRule("Team.TimeoutsPer", "true")
        rdclHalf.set(READONLY, true)
        add(Rulesets.RULESET, rdclHalf)
    }

    override fun getRuleDefinition(k: String): RuleDefinition {
        return get(Rulesets.RULE_DEFINITION, k)
    }

    override fun getRuleset(id: String): Ruleset {
        synchronized(coreLock) {
            var r = get(Rulesets.RULESET, id)
            if (r == null) {
                r = get(Rulesets.RULESET, Rulesets.ROOT_ID)
            }
            return r
        }
    }

    override fun addRuleset(name: String, parentRs: Ruleset): Ruleset {
        return addRuleset(name, parentRs, UUID.randomUUID().toString())
    }

    override fun addRuleset(name: String, parentRs: Ruleset, id: String): Ruleset {
        synchronized(coreLock) {
            val r: Ruleset = RulesetImpl(this, name, parentRs, id)
            add(Rulesets.RULESET, r)
            return r
        }
    }

    override fun removeRuleset(id: String) {
        remove(Rulesets.RULESET, id)
    }

    inner class RulesetImpl(rulesets: Rulesets, name: String, parent: Ruleset?, id: String) :
        ScoreBoardEventProviderImpl<Ruleset?>(rulesets, id, Rulesets.RULESET), Ruleset {
        init {
            addProperties(Ruleset.props)
            set(Ruleset.NAME, name)
            set(Ruleset.PARENT, parent)
        }

        override fun computeValue(
            prop: Value<*>,
            value: Any?,
            last: Any?,
            source: ScoreBoardEventProvider.Source,
            flag: ScoreBoardEventProvider.Flag?
        ): Any? {
            if (prop === Ruleset.PARENT && this.isAncestorOf(value as? Ruleset)) {
                return last
            }
            return value
        }

        override fun get(r: Rule): String {
            return get(Ruleset.RULE, r.toString()).value
        }

        override fun getName(): String? {
            return get(Ruleset.NAME)
        }

        override fun setName(n: String) {
            set(Ruleset.NAME, n)
        }

        override fun getParentRuleset(): Ruleset? {
            return get(Ruleset.PARENT)
        }

        override fun setParentRuleset(rs: Ruleset) {
            set(Ruleset.PARENT, rs)
        }

        override fun isAncestorOf(rs: Ruleset?): Boolean {
            if (rs == null) {
                return false
            }
            val parentRs = rs.parentRuleset
            return this === rs || this.isAncestorOf(parentRs)
        }

        override fun setRule(id: String, value: String) {
            add(Ruleset.RULE, ValWithId(id, value))
        }
    }
}
