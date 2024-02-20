package org.kdb.inside.brains.view;

import org.kdb.inside.brains.settings.KdbSettingsService;
import org.kdb.inside.brains.view.console.ConsoleOptions;
import org.kdb.inside.brains.view.console.NumericalOptions;

import java.math.RoundingMode;
import java.util.function.BooleanSupplier;

public class FormatterOptions {
    private final ConsoleOptions consoleOptions;
    private final NumericalOptions numericalOptions;

    private final BooleanSupplier thousandsSupplier;
    private final BooleanSupplier scientificSupplier;

    public FormatterOptions() {
        this(KdbSettingsService.getInstance().getConsoleOptions(), KdbSettingsService.getInstance().getNumericalOptions());
    }

    public FormatterOptions(ConsoleOptions consoleOptions, NumericalOptions numericalOptions) {
        this(consoleOptions, numericalOptions, () -> false, numericalOptions::isScientificNotation);
    }

    private FormatterOptions(ConsoleOptions consoleOptions, NumericalOptions numericalOptions, BooleanSupplier thousandsSupplier, BooleanSupplier scientificSupplier) {
        this.consoleOptions = consoleOptions;
        this.numericalOptions = numericalOptions;
        this.thousandsSupplier = thousandsSupplier;
        this.scientificSupplier = scientificSupplier;
    }

    public boolean isWrapStrings() {
        return consoleOptions.isWrapStrings();
    }

    public boolean isPrefixSymbols() {
        return consoleOptions.isPrefixSymbols();
    }

    public boolean isEnlistArrays() {
        return consoleOptions.isEnlistArrays();
    }

    public int getFloatPrecision() {
        return numericalOptions.getFloatPrecision();
    }

    public RoundingMode getRoundingMode() {
        return numericalOptions.getRoundingMode();
    }

    public boolean isScientificNotation() {
        return scientificSupplier.getAsBoolean();
    }

    public boolean isThousandsSeparator() {
        return thousandsSupplier.getAsBoolean();
    }

    public FormatterOptions withThousandsSeparator(BooleanSupplier thousands) {
        return new FormatterOptions(consoleOptions, numericalOptions, thousands, scientificSupplier);
    }

    public FormatterOptions withScientificNotation(BooleanSupplier scientific) {
        return new FormatterOptions(consoleOptions, numericalOptions, thousandsSupplier, scientific);
    }

    public FormatterOptions withThousandAndScientific(BooleanSupplier thousands, BooleanSupplier scientific) {
        return new FormatterOptions(consoleOptions, numericalOptions, thousands, scientific);
    }
}