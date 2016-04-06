/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package tpt.dbweb.cat.datatypes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Fraction {

  private final double nominator;

  private final double denominator;

  public static Fraction ONE = new Fraction(1., 1.);

  public static Fraction ZERO = new Fraction(0., 1.);

  public static Fraction EMPTY = new Fraction(0., 0.);

  @JsonCreator
  public Fraction(@JsonProperty("nom") double nominator, @JsonProperty("denom") double denominator) {
    this.nominator = nominator;
    this.denominator = denominator;
  }

  @JsonProperty("nom")
  public double getNominator() {
    return nominator;
  }

  @JsonProperty("denom")
  public double getDenominator() {
    return denominator;
  }

  @JsonIgnore
  public double value() {
    return nominator / denominator;
  }

  public double value(double divisionByZeroValue) {
    return (denominator == 0) ? divisionByZeroValue : nominator / denominator;
  }

  public Fraction multiply(Fraction f) {
    return new Fraction(this.nominator * f.nominator, this.denominator * f.denominator);
  }

  public Fraction divide(Fraction f) {
    return new Fraction(this.nominator / f.nominator, this.denominator / f.denominator);
  }

  public Fraction add(Fraction f) {
    double nominator = 0, denominator = 0;
    if (this.denominator != 0) {
      nominator = this.nominator / this.denominator;
      denominator = 1;
    }
    if (f.denominator != 0) {
      nominator += f.nominator / f.denominator;
      denominator = 1;
    }
    return new Fraction(nominator, denominator);
  }

  public Fraction addMicro(Fraction f) {
    return new Fraction(this.nominator + f.nominator, this.denominator + f.denominator);
  }

  @Override
  public String toString() {
    return nominator + "/" + denominator;
  }
}
