/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.core;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.freemarker.test.TemplateTest;
import org.apache.freemarker.test.TestConfigurationBuilder;
import org.junit.Test;

public class InterpolationSyntaxTest extends TemplateTest {

    @Test
    public void fm2HashInterpolationNotRecognized() throws IOException, TemplateException {
        // Find related: [interpolation prefixes]
        assertOutput("#{1} ${1} ${'#{2} ${2}'}", "#{1} 1 #{2} 2");
    }

    @Test
    public void dollarInterpolationSyntaxTest() throws Exception {
        assertOutput("${1} #{1} [=1]", "1 #{1} [=1]");
        assertOutput(
                "${{'x': 1}['x']} #{{'x': 1}['x']} [={'x': 1}['x']]",
                "1 #{{'x': 1}['x']} [={'x': 1}['x']]");
        
        assertOutput("${'a[=1]b'}", "a[=1]b");
        assertOutput("${'a${1}#{2}b'}", "a1#{2}b");
        assertOutput("${'a${1}#{2}b[=3]'}", "a1#{2}b[=3]");
        
        assertOutput("<@r'${1} #{1} [=1]'?interpret />", "1 #{1} [=1]");
        assertOutput("${'\"${1} #{1} [=1]\"'?eval}", "1 #{1} [=1]");
        
        assertOutput("<#setting booleanFormat='y,n'>${2>1}", "y"); // Not an error since 2.3.28
        assertOutput("[#ftl][#setting booleanFormat='y,n']${2>1}", "y"); // Not an error since 2.3.28
    }

    @Test
    public void squareBracketInterpolationSyntaxTest() throws Exception {
        setConfiguration(new TestConfigurationBuilder()
                .interpolationSyntax(InterpolationSyntax.SQUARE_BRACKET)
                .build());
        
        assertOutput("${1} #{1} [=1]", "${1} #{1} 1");
        assertOutput(
                "${{'x': 1}['x']} #{{'x': 1}['x']} [={'x': 1}['x']]",
                "${{'x': 1}['x']} #{{'x': 1}['x']} 1");

        assertOutput("[=1]][=2]]", "1]2]");
        assertOutput("[= 1 ][= <#-- c --> 2 <#-- c --> ]", "12");
        assertOutput("[ =1]", "[ =1]");
        
        assertErrorContains("<#if [true][0]]></#if>", "\"]\"", "nothing open");

        setConfiguration(new TestConfigurationBuilder()
                .tagSyntax(TagSyntax.SQUARE_BRACKET)
                .interpolationSyntax(InterpolationSyntax.SQUARE_BRACKET)
                .build());
        assertOutput("[#if [true][0]]>[/#if]", ">");
        assertOutput("[=1][=2]${3}", "12${3}");
        setConfiguration(new TestConfigurationBuilder()
                .tagSyntax(TagSyntax.ANGLE_BRACKET)
                .interpolationSyntax(InterpolationSyntax.SQUARE_BRACKET)
                .build());
        assertOutput("[#ftl][#if [true][0]]>[/#if]", ">");
        assertOutput("[#ftl][=1][=2]${3}", "12${3}");
         
        assertOutput("[='a[=1]b']", "a1b");
        assertOutput("[='a${1}#{2}b']", "a${1}#{2}b");
        assertOutput("[='a${1}#{2}b[=3]']", "a${1}#{2}b3");
        
        assertOutput("<@r'${1} #{1} [=1]'?interpret />", "${1} #{1} 1");
        assertOutput("[='\"${1} #{1} [=1]\"'?eval]", "${1} #{1} 1");
        
        assertErrorContains("[=", "end of file");
        assertErrorContains("[=1", "unclosed \"[\"");
        assertErrorContains("[=1}", "\"}\"", "open");
        
        assertOutput("[='[\\=1]']", "[=1]");
        assertOutput("[='[\\=1][=2]']", "12"); // Usual legacy interpolation escaping glitch...
        assertOutput("[=r'[=1]']", "[=1]");
        
        assertOutput("<#setting booleanFormat='y,n'>[=2>1]", "y");
        assertOutput("[#ftl][#setting booleanFormat='y,n'][=2>1]", "y");
        
        StringWriter sw = new StringWriter();
        new Template(null, "[= 1 + '[= 2 ]' ]", getConfiguration()).dump(sw);
        assertEquals("[=1 + \"[=2]\"]", sw.toString());
    }

    @Test
    public void squareBracketTagSyntaxStillWorks() throws Exception {
        for (InterpolationSyntax intepolationSyntax : new InterpolationSyntax[] {
                InterpolationSyntax.DOLLAR, InterpolationSyntax.SQUARE_BRACKET }) {
            setConfiguration(new TestConfigurationBuilder()
                    .tagSyntax(TagSyntax.SQUARE_BRACKET)
                    .interpolationSyntax(intepolationSyntax)
                    .build());
            
            assertOutput("[#if [true][0]]t[#else]f[/#if]", "t");
            assertOutput("[@r'[#if [true][0]]t[#else]f[/#if]'?interpret /]", "t");
        }
    }
    
    @Test
    public void legacyTagSyntaxGlitchFixedTest() throws Exception {
        String badFtl1 = "<#if [true][0]]OK</#if>";
        String badFtl2 = "<#if true>OK</#if]";
        String badFtl3 = "<#assign x = 'OK'/]${x}";
        String badFtl4 = " <#t/]OK\n";
        
        setConfiguration(new TestConfigurationBuilder()
                .interpolationSyntax(InterpolationSyntax.DOLLAR)
                .build());
        assertErrorContains(badFtl1, "\"]\"");
        assertErrorContains(badFtl2, "\"]\"");
        assertErrorContains(badFtl3, "\"]\"");
        assertErrorContains(badFtl4, "\"]\"");
        
        // Glitch is not emulated with this:
        setConfiguration(new TestConfigurationBuilder()
                .interpolationSyntax(InterpolationSyntax.SQUARE_BRACKET)
                .build());
        assertErrorContains(badFtl1, "\"]\"");
        assertErrorContains(badFtl2, "\"]\"");
        assertErrorContains(badFtl3, "\"]\"");
        assertErrorContains(badFtl4, "\"]\"");
    }

    @Test
    public void errorMessagesAreSquareBracketInterpolationSyntaxAwareTest() throws Exception {
        assertErrorContains("<#if ${x}></#if>", "${...}", "${myExpression}");
        assertErrorContains("<#if [=x]></#if>", "[=...]", "[=myExpression]");
    }

    @Test
    public void unclosedSyntaxErrorTest() throws Exception {
        assertErrorContains("${1", "unclosed \"{\"");
        
        setConfiguration(new TestConfigurationBuilder()
                .interpolationSyntax(InterpolationSyntax.SQUARE_BRACKET)
                .build());
        assertErrorContains("[=1", "unclosed \"[\"");
    }
    
}