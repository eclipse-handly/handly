/*
 * generated by Xtext
 */
grammar InternalFoo;

options {
	superClass=AbstractInternalContentAssistParser;
}

@lexer::header {
package org.eclipse.handly.examples.basic.ide.contentassist.antlr.internal;

// Hack: Use our own Lexer superclass by means of import. 
// Currently there is no other way to specify the superclass for the lexer.
import org.eclipse.xtext.ide.editor.contentassist.antlr.internal.Lexer;
}

@parser::header {
package org.eclipse.handly.examples.basic.ide.contentassist.antlr.internal;

import java.io.InputStream;
import org.eclipse.xtext.*;
import org.eclipse.xtext.parser.*;
import org.eclipse.xtext.parser.impl.*;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.parser.antlr.XtextTokenStream;
import org.eclipse.xtext.parser.antlr.XtextTokenStream.HiddenTokens;
import org.eclipse.xtext.ide.editor.contentassist.antlr.internal.AbstractInternalContentAssistParser;
import org.eclipse.xtext.ide.editor.contentassist.antlr.internal.DFA;
import org.eclipse.handly.examples.basic.services.FooGrammarAccess;

}
@parser::members {
	private FooGrammarAccess grammarAccess;

	public void setGrammarAccess(FooGrammarAccess grammarAccess) {
		this.grammarAccess = grammarAccess;
	}

	@Override
	protected Grammar getGrammar() {
		return grammarAccess.getGrammar();
	}

	@Override
	protected String getValueForTokenName(String tokenName) {
		return tokenName;
	}
}

// Entry rule entryRuleUnit
entryRuleUnit
:
{ before(grammarAccess.getUnitRule()); }
	 ruleUnit
{ after(grammarAccess.getUnitRule()); } 
	 EOF 
;

// Rule Unit
ruleUnit 
	@init {
		int stackSize = keepStackSize();
	}
	:
	(
		{ before(grammarAccess.getUnitAccess().getGroup()); }
		(rule__Unit__Group__0)
		{ after(grammarAccess.getUnitAccess().getGroup()); }
	)
;
finally {
	restoreStackSize(stackSize);
}

// Entry rule entryRuleVar
entryRuleVar
:
{ before(grammarAccess.getVarRule()); }
	 ruleVar
{ after(grammarAccess.getVarRule()); } 
	 EOF 
;

// Rule Var
ruleVar 
	@init {
		int stackSize = keepStackSize();
	}
	:
	(
		{ before(grammarAccess.getVarAccess().getGroup()); }
		(rule__Var__Group__0)
		{ after(grammarAccess.getVarAccess().getGroup()); }
	)
;
finally {
	restoreStackSize(stackSize);
}

// Entry rule entryRuleDef
entryRuleDef
:
{ before(grammarAccess.getDefRule()); }
	 ruleDef
{ after(grammarAccess.getDefRule()); } 
	 EOF 
;

// Rule Def
ruleDef 
	@init {
		int stackSize = keepStackSize();
	}
	:
	(
		{ before(grammarAccess.getDefAccess().getGroup()); }
		(rule__Def__Group__0)
		{ after(grammarAccess.getDefAccess().getGroup()); }
	)
;
finally {
	restoreStackSize(stackSize);
}

rule__Unit__Group__0
	@init {
		int stackSize = keepStackSize();
	}
:
	rule__Unit__Group__0__Impl
	rule__Unit__Group__1
;
finally {
	restoreStackSize(stackSize);
}

rule__Unit__Group__0__Impl
	@init {
		int stackSize = keepStackSize();
	}
:
(
	{ before(grammarAccess.getUnitAccess().getVarsAssignment_0()); }
	(rule__Unit__VarsAssignment_0)*
	{ after(grammarAccess.getUnitAccess().getVarsAssignment_0()); }
)
;
finally {
	restoreStackSize(stackSize);
}

rule__Unit__Group__1
	@init {
		int stackSize = keepStackSize();
	}
:
	rule__Unit__Group__1__Impl
;
finally {
	restoreStackSize(stackSize);
}

rule__Unit__Group__1__Impl
	@init {
		int stackSize = keepStackSize();
	}
:
(
	{ before(grammarAccess.getUnitAccess().getDefsAssignment_1()); }
	(rule__Unit__DefsAssignment_1)*
	{ after(grammarAccess.getUnitAccess().getDefsAssignment_1()); }
)
;
finally {
	restoreStackSize(stackSize);
}


rule__Var__Group__0
	@init {
		int stackSize = keepStackSize();
	}
:
	rule__Var__Group__0__Impl
	rule__Var__Group__1
;
finally {
	restoreStackSize(stackSize);
}

rule__Var__Group__0__Impl
	@init {
		int stackSize = keepStackSize();
	}
:
(
	{ before(grammarAccess.getVarAccess().getVarKeyword_0()); }
	'var'
	{ after(grammarAccess.getVarAccess().getVarKeyword_0()); }
)
;
finally {
	restoreStackSize(stackSize);
}

rule__Var__Group__1
	@init {
		int stackSize = keepStackSize();
	}
:
	rule__Var__Group__1__Impl
	rule__Var__Group__2
;
finally {
	restoreStackSize(stackSize);
}

rule__Var__Group__1__Impl
	@init {
		int stackSize = keepStackSize();
	}
:
(
	{ before(grammarAccess.getVarAccess().getNameAssignment_1()); }
	(rule__Var__NameAssignment_1)
	{ after(grammarAccess.getVarAccess().getNameAssignment_1()); }
)
;
finally {
	restoreStackSize(stackSize);
}

rule__Var__Group__2
	@init {
		int stackSize = keepStackSize();
	}
:
	rule__Var__Group__2__Impl
;
finally {
	restoreStackSize(stackSize);
}

rule__Var__Group__2__Impl
	@init {
		int stackSize = keepStackSize();
	}
:
(
	{ before(grammarAccess.getVarAccess().getSemicolonKeyword_2()); }
	';'
	{ after(grammarAccess.getVarAccess().getSemicolonKeyword_2()); }
)
;
finally {
	restoreStackSize(stackSize);
}


rule__Def__Group__0
	@init {
		int stackSize = keepStackSize();
	}
:
	rule__Def__Group__0__Impl
	rule__Def__Group__1
;
finally {
	restoreStackSize(stackSize);
}

rule__Def__Group__0__Impl
	@init {
		int stackSize = keepStackSize();
	}
:
(
	{ before(grammarAccess.getDefAccess().getDefKeyword_0()); }
	'def'
	{ after(grammarAccess.getDefAccess().getDefKeyword_0()); }
)
;
finally {
	restoreStackSize(stackSize);
}

rule__Def__Group__1
	@init {
		int stackSize = keepStackSize();
	}
:
	rule__Def__Group__1__Impl
	rule__Def__Group__2
;
finally {
	restoreStackSize(stackSize);
}

rule__Def__Group__1__Impl
	@init {
		int stackSize = keepStackSize();
	}
:
(
	{ before(grammarAccess.getDefAccess().getNameAssignment_1()); }
	(rule__Def__NameAssignment_1)
	{ after(grammarAccess.getDefAccess().getNameAssignment_1()); }
)
;
finally {
	restoreStackSize(stackSize);
}

rule__Def__Group__2
	@init {
		int stackSize = keepStackSize();
	}
:
	rule__Def__Group__2__Impl
	rule__Def__Group__3
;
finally {
	restoreStackSize(stackSize);
}

rule__Def__Group__2__Impl
	@init {
		int stackSize = keepStackSize();
	}
:
(
	{ before(grammarAccess.getDefAccess().getLeftParenthesisKeyword_2()); }
	'('
	{ after(grammarAccess.getDefAccess().getLeftParenthesisKeyword_2()); }
)
;
finally {
	restoreStackSize(stackSize);
}

rule__Def__Group__3
	@init {
		int stackSize = keepStackSize();
	}
:
	rule__Def__Group__3__Impl
	rule__Def__Group__4
;
finally {
	restoreStackSize(stackSize);
}

rule__Def__Group__3__Impl
	@init {
		int stackSize = keepStackSize();
	}
:
(
	{ before(grammarAccess.getDefAccess().getParamsAssignment_3()); }
	(rule__Def__ParamsAssignment_3)?
	{ after(grammarAccess.getDefAccess().getParamsAssignment_3()); }
)
;
finally {
	restoreStackSize(stackSize);
}

rule__Def__Group__4
	@init {
		int stackSize = keepStackSize();
	}
:
	rule__Def__Group__4__Impl
	rule__Def__Group__5
;
finally {
	restoreStackSize(stackSize);
}

rule__Def__Group__4__Impl
	@init {
		int stackSize = keepStackSize();
	}
:
(
	{ before(grammarAccess.getDefAccess().getGroup_4()); }
	(rule__Def__Group_4__0)*
	{ after(grammarAccess.getDefAccess().getGroup_4()); }
)
;
finally {
	restoreStackSize(stackSize);
}

rule__Def__Group__5
	@init {
		int stackSize = keepStackSize();
	}
:
	rule__Def__Group__5__Impl
	rule__Def__Group__6
;
finally {
	restoreStackSize(stackSize);
}

rule__Def__Group__5__Impl
	@init {
		int stackSize = keepStackSize();
	}
:
(
	{ before(grammarAccess.getDefAccess().getRightParenthesisKeyword_5()); }
	')'
	{ after(grammarAccess.getDefAccess().getRightParenthesisKeyword_5()); }
)
;
finally {
	restoreStackSize(stackSize);
}

rule__Def__Group__6
	@init {
		int stackSize = keepStackSize();
	}
:
	rule__Def__Group__6__Impl
	rule__Def__Group__7
;
finally {
	restoreStackSize(stackSize);
}

rule__Def__Group__6__Impl
	@init {
		int stackSize = keepStackSize();
	}
:
(
	{ before(grammarAccess.getDefAccess().getLeftCurlyBracketKeyword_6()); }
	'{'
	{ after(grammarAccess.getDefAccess().getLeftCurlyBracketKeyword_6()); }
)
;
finally {
	restoreStackSize(stackSize);
}

rule__Def__Group__7
	@init {
		int stackSize = keepStackSize();
	}
:
	rule__Def__Group__7__Impl
;
finally {
	restoreStackSize(stackSize);
}

rule__Def__Group__7__Impl
	@init {
		int stackSize = keepStackSize();
	}
:
(
	{ before(grammarAccess.getDefAccess().getRightCurlyBracketKeyword_7()); }
	'}'
	{ after(grammarAccess.getDefAccess().getRightCurlyBracketKeyword_7()); }
)
;
finally {
	restoreStackSize(stackSize);
}


rule__Def__Group_4__0
	@init {
		int stackSize = keepStackSize();
	}
:
	rule__Def__Group_4__0__Impl
	rule__Def__Group_4__1
;
finally {
	restoreStackSize(stackSize);
}

rule__Def__Group_4__0__Impl
	@init {
		int stackSize = keepStackSize();
	}
:
(
	{ before(grammarAccess.getDefAccess().getCommaKeyword_4_0()); }
	','
	{ after(grammarAccess.getDefAccess().getCommaKeyword_4_0()); }
)
;
finally {
	restoreStackSize(stackSize);
}

rule__Def__Group_4__1
	@init {
		int stackSize = keepStackSize();
	}
:
	rule__Def__Group_4__1__Impl
;
finally {
	restoreStackSize(stackSize);
}

rule__Def__Group_4__1__Impl
	@init {
		int stackSize = keepStackSize();
	}
:
(
	{ before(grammarAccess.getDefAccess().getParamsAssignment_4_1()); }
	(rule__Def__ParamsAssignment_4_1)
	{ after(grammarAccess.getDefAccess().getParamsAssignment_4_1()); }
)
;
finally {
	restoreStackSize(stackSize);
}


rule__Unit__VarsAssignment_0
	@init {
		int stackSize = keepStackSize();
	}
:
	(
		{ before(grammarAccess.getUnitAccess().getVarsVarParserRuleCall_0_0()); }
		ruleVar
		{ after(grammarAccess.getUnitAccess().getVarsVarParserRuleCall_0_0()); }
	)
;
finally {
	restoreStackSize(stackSize);
}

rule__Unit__DefsAssignment_1
	@init {
		int stackSize = keepStackSize();
	}
:
	(
		{ before(grammarAccess.getUnitAccess().getDefsDefParserRuleCall_1_0()); }
		ruleDef
		{ after(grammarAccess.getUnitAccess().getDefsDefParserRuleCall_1_0()); }
	)
;
finally {
	restoreStackSize(stackSize);
}

rule__Var__NameAssignment_1
	@init {
		int stackSize = keepStackSize();
	}
:
	(
		{ before(grammarAccess.getVarAccess().getNameIDTerminalRuleCall_1_0()); }
		RULE_ID
		{ after(grammarAccess.getVarAccess().getNameIDTerminalRuleCall_1_0()); }
	)
;
finally {
	restoreStackSize(stackSize);
}

rule__Def__NameAssignment_1
	@init {
		int stackSize = keepStackSize();
	}
:
	(
		{ before(grammarAccess.getDefAccess().getNameIDTerminalRuleCall_1_0()); }
		RULE_ID
		{ after(grammarAccess.getDefAccess().getNameIDTerminalRuleCall_1_0()); }
	)
;
finally {
	restoreStackSize(stackSize);
}

rule__Def__ParamsAssignment_3
	@init {
		int stackSize = keepStackSize();
	}
:
	(
		{ before(grammarAccess.getDefAccess().getParamsIDTerminalRuleCall_3_0()); }
		RULE_ID
		{ after(grammarAccess.getDefAccess().getParamsIDTerminalRuleCall_3_0()); }
	)
;
finally {
	restoreStackSize(stackSize);
}

rule__Def__ParamsAssignment_4_1
	@init {
		int stackSize = keepStackSize();
	}
:
	(
		{ before(grammarAccess.getDefAccess().getParamsIDTerminalRuleCall_4_1_0()); }
		RULE_ID
		{ after(grammarAccess.getDefAccess().getParamsIDTerminalRuleCall_4_1_0()); }
	)
;
finally {
	restoreStackSize(stackSize);
}

RULE_ID : '^'? ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'_'|'0'..'9')*;

RULE_INT : ('0'..'9')+;

RULE_STRING : ('"' ('\\' .|~(('\\'|'"')))* '"'|'\'' ('\\' .|~(('\\'|'\'')))* '\'');

RULE_ML_COMMENT : '/*' ( options {greedy=false;} : . )*'*/';

RULE_SL_COMMENT : '//' ~(('\n'|'\r'))* ('\r'? '\n')?;

RULE_WS : (' '|'\t'|'\r'|'\n')+;

RULE_ANY_OTHER : .;
