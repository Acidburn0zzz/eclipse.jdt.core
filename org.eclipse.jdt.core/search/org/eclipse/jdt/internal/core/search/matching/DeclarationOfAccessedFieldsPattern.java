package org.eclipse.jdt.internal.core.search.matching;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.util.CharOperation;
import org.eclipse.jdt.internal.compiler.util.ObjectSet;

public class DeclarationOfAccessedFieldsPattern extends FieldReferencePattern {
	ObjectSet knownFields;
public DeclarationOfAccessedFieldsPattern(
	char[] name, 
	int matchMode, 
	boolean isCaseSensitive,
	char[] declaringQualification,
	char[] declaringSimpleName,	
	char[] typeQualification, 
	char[] typeSimpleName,
	boolean readAccess,
	boolean writeAccess) {
		
	super(
		name, 
		matchMode, 
		isCaseSensitive, 
		declaringQualification, 
		declaringSimpleName,
		typeQualification,
		typeSimpleName,
		readAccess,
		writeAccess);
	this.needsResolve = true;
	this.knownFields = new ObjectSet();
}

/**
 * @see SearchPattern#matchReportReference
 */
protected void matchReportReference(AstNode reference, IJavaElement element, int accuracy, MatchLocator locator) throws CoreException {
	// need accurate match to be able to open on type ref
	if (accuracy == IJavaSearchResultCollector.POTENTIAL_MATCH) return;
	
	if (reference instanceof FieldReference) {
		this.reportDeclaration(((FieldReference)reference).binding, locator);
	} else if (reference instanceof QualifiedNameReference) {
		QualifiedNameReference qNameRef = (QualifiedNameReference)reference;
		Binding binding = qNameRef.binding;
		if (binding instanceof FieldBinding) {
			this.reportDeclaration((FieldBinding)binding, locator);
		} 
		int otherMax = qNameRef.otherBindings == null ? 0 : qNameRef.otherBindings.length;
		for (int i = 0; i < otherMax; i++){
			this.reportDeclaration(qNameRef.otherBindings[i], locator);
		}
	} else if (reference instanceof SingleNameReference) {
		this.reportDeclaration(
			(FieldBinding)((SingleNameReference)reference).binding, 
			locator);
	}
}
private void reportDeclaration(FieldBinding fieldBinding, MatchLocator locator) throws CoreException {
	ReferenceBinding declaringClass = fieldBinding.declaringClass;
	IType type = locator.lookupType(declaringClass);
	if (type == null) return; // case of a secondary type
	char[] name = fieldBinding.name;
	IField field = type.getField(new String(name));
	if (this.knownFields.contains(field)) return;
	this.knownFields.add(field);
	IResource resource = type.getUnderlyingResource();
	boolean isBinary = type.isBinary();
	IBinaryType info = null;
	if (isBinary) {
		if (resource == null) {
			resource = type.getJavaProject().getProject();
		}
		info = locator.getBinaryInfo((org.eclipse.jdt.internal.core.ClassFile)type.getClassFile(), resource);
		locator.reportBinaryMatch(resource, field, info, IJavaSearchResultCollector.EXACT_MATCH);
	} else {
		TypeDeclaration typeDecl = ((SourceTypeBinding)declaringClass).scope.referenceContext;
		FieldDeclaration fieldDecl = null;
		FieldDeclaration[] fieldDecls = typeDecl.fields;
		for (int i = 0, length = fieldDecls.length; i < length; i++) {
			if (CharOperation.equals(name, fieldDecls[i].name)) {
				fieldDecl = fieldDecls[i];
				break;
			}
		} 
		if (fieldDecl != null) {
			locator.report(resource, fieldDecl.sourceStart, fieldDecl.sourceEnd, field, IJavaSearchResultCollector.EXACT_MATCH);
		}
	}
}
}
