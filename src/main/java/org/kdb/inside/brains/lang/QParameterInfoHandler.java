package org.kdb.inside.brains.lang;

@Deprecated
public class QParameterInfoHandler {
//    implements ParameterInfoHandlerWithTabActionSupport<QArguments, Object, QExpression>
//} {


/*

    private static final Pattern PARAM = Pattern.compile("@param\\s+(\\w+)\\s+(\\(.+\\))");

    @Override
    public QExpression @NotNull [] getActualParameters(@NotNull QArguments o) {
        return new QExpression[0];
    }

    @NotNull
    @Override
    public IElementType getActualParameterDelimiterType() {
        return QTypes.SEMICOLON;
    }

    @NotNull
    @Override
    public IElementType getActualParametersRBraceType() {
        return QTypes.BRACKET_CLOSE;
    }

    @Override
    public boolean isWhitespaceSensitive() {
        return false;
    }

    @Override
    public @NotNull Set<Class<?>> getArgumentListAllowedParentClasses() {
        return Set.of();
    }

    @Override
    public @NotNull Set<? extends Class<?>> getArgListStopSearchClasses() {
        return Set.of();
    }

    @NotNull
    @Override
    public Class<QArguments> getArgumentListClass() {
        return QArguments.class;
    }

    @Override
    public boolean couldShowInLookup() {
        return false;
    }

    @Nullable
    @Override
    public Object[] getParametersForLookup(LookupElement item, ParameterInfoContext context) {
//        item.putCopyableUserData(null, null);
        return new Object[0];
    }

    @Nullable
    @Override
    public QArguments findElementForParameterInfo(@NotNull CreateParameterInfoContext context) {
        PsiElement element = context.getFile().findElementAt(context.getOffset());
        if (element == null) {
            return null;
        }
        final QArguments args = PsiTreeUtil.getParentOfType(element, QArguments.class);
        if (args == null) {
            return null;
        }
        final QVariable declaration = Optional.ofNullable(PsiTreeUtil.getPrevSiblingOfType(args, QVariable.class))
                .map(id -> new VariableReference(id).resolveFirstUnordered())
                .filter(QVariable.class::isInstance)
                .map(QVariable.class::cast)
                .orElse(null);
        if (declaration == null) {
            return null;
        }
        return Optional.of(declaration)
                .map(PsiElement::getParent)
                .filter(QAssignment.class::isInstance)
                .map(QAssignment.class::cast)
                .map(QAssignment::getExpression)
                .map(e -> PsiTreeUtil.findChildOfType(e, QLambda.class))
                .map(lambda -> {
                    KParameterInfo info = new KParameterInfo(declaration, lambda, args);
                    context.setItemsToShow(new Object[]{info});
                    return args;
                })
                .orElse(null);
    }

    @Override
    public void showParameterInfo(@NotNull QArguments element, @NotNull CreateParameterInfoContext context) {
        context.showHint(element, context.getOffset(), this);
    }

    @Nullable
    @Override
    public QArguments findElementForUpdatingParameterInfo(@NotNull UpdateParameterInfoContext context) {
        if (isOutsideOfInvocation(context)) {
            return null;
        }
        PsiElement element = context.getFile().findElementAt(context.getOffset());
        if (element == null) {
            return null;
        }
        return PsiTreeUtil.getParentOfType(element, QArguments.class);
    }

    private boolean isOutsideOfInvocation(UpdateParameterInfoContext context) {
        PsiElement currentElement = context.getFile().findElementAt(context.getOffset());
        PsiElement currentArgs = PsiTreeUtil.getParentOfType(currentElement, QArguments.class);
        PsiElement expectedArgs = context.getParameterOwner();
        return !Objects.equals(expectedArgs, currentArgs);
    }

    @Override
    public void updateParameterInfo(@NotNull QArguments args, @NotNull UpdateParameterInfoContext context) {
        int i = 0;
        QExpression arg = PsiTreeUtil.getChildOfType(args, QExpression.class);
        while (arg != null && arg.getTextOffset() + arg.getTextLength() + ";".length() <= context.getOffset()) {
            i++;
            arg = PsiTreeUtil.getNextSiblingOfType(arg, QExpression.class);
        }
        context.setCurrentParameter(i);
    }

    @Override
    public void updateUI(KParameterInfo p, @NotNull ParameterInfoUIContext context) {
        final QParameters params = p.params.getParameters();
        final QVariable id = p.declaration;
        if (params == null) { // implicit x;y;z params
            setupParameterInfoPresentation(id, Arrays.asList("x", "y", "z"), context);
            return;
        }
        if (params.getVariableList().isEmpty()) { // no params
            context.setupRawUIComponentPresentation("no params");
            return;
        }
        final List<String> paramNames = params.getVariableList().stream().map(QVariable::getName).collect(Collectors.toList());
        setupParameterInfoPresentation(id, paramNames, context);
    }

    private void setupParameterInfoPresentation(QVariable id, List<String> names, ParameterInfoUIContext context) {
        final Map<String, String> types = new HashMap<>();
        for (String line : KDocumentationProvider.getFunctionDocs(id)) {
            final Matcher matcher = PARAM.matcher(line);
            if (!matcher.find()) {
                continue;
            }
            final String n = matcher.group(1);
            final String t = matcher.group(2);
            types.put(n, t);
        }
        final List<String> namesAndTypes = new ArrayList<>();
        for (String name : names) {
            if (types.containsKey(name)) {
                namesAndTypes.add(name + " " + types.get(name));
            } else {
                namesAndTypes.add(name);
            }
        }
        final String display = String.format("%s[%s]", id.getQualifiedName(), String.join(";", namesAndTypes));
        int i = context.getCurrentParameterIndex();
        int start = -1, end = -1;
        if (i > -1 && i < namesAndTypes.size()) {
            start = display.indexOf('[') + 1;
            for (int j = 0; j < i; j++) {
                String name = namesAndTypes.get(j);
                start += name.length() + ";".length();
            }
            end = start + namesAndTypes.get(i).length();
        }
        context.setupUIComponentPresentation(display, start, end, false, false, false, context.getDefaultParameterColor());
    }

    static class KParameterInfo {
        final QVariable declaration;
        final QLambda params;
        final QArguments arguments;

        KParameterInfo(QVariable declaration, QLambda params, QArguments arguments) {
            this.declaration = declaration;
            this.params = params;
            this.arguments = arguments;
        }
    }
*/
}
