package org.apache.spark.mllib.regression

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.optimization._
import org.apache.spark.mllib.util.MLUtils

import org.jblas.DoubleMatrix

/**
 * Regression model trained using LinearRegression.
 *
 * @param weights Weights computed for every feature.
 * @param intercept Intercept computed for this model.
 */
class LinearRegressionModel(
  override val weights: Array[Double],
  override val intercept: Double)
  extends GeneralizedLinearModel(weights, intercept) with RegressionModel with Serializable {

  // prediction: the 1st difference between LinearRegressionModel and LogisticRegressionModel
  // @chenwq
  override def predictPoint(
    dataMatrix: DoubleMatrix,
    weightMatrix: DoubleMatrix,
    intercept: Double): Double = {
    dataMatrix.dot(weightMatrix) + intercept
  }
}

/**
 * Train a linear regression model with no regularization using Stochastic Gradient Descent.
 */
class LinearRegressionWithSGD private (
  var stepSize: Double,
  var numIterations: Int,
  var miniBatchFraction: Double)
  extends GeneralizedLinearAlgorithm[LinearRegressionModel] with Serializable {

  // gradient: the 2nd difference between LinearRegressionModel and LogisticRegressionModel
  // according to the loss function between them:
  // square loss & cross-entropy
  // @chenwq
  val gradient = new SquaredGradient()
  val updater = new SimpleUpdater()
  val optimizer = new GradientDescent(gradient, updater)
  	.setStepSize(stepSize)
    .setNumIterations(numIterations)
    .setMiniBatchFraction(miniBatchFraction)

  /**
   * Construct a LinearRegression object with default parameters
   */
  def this() = this(1.0, 100, 1.0)

  override def createModel(weights: Array[Double], intercept: Double) = {
    new LinearRegressionModel(weights, intercept)
  }
}

/**
 * Top-level methods for calling LinearRegression.
 */
object LinearRegressionWithSGD {

  /**
   * Train a Linear Regression model given an RDD of (label, features) pairs. We run a fixed number
   * of iterations of gradient descent using the specified step size. Each iteration uses
   * `miniBatchFraction` fraction of the data to calculate the gradient. The weights used in
   * gradient descent are initialized using the initial weights provided.
   *
   * @param input RDD of (label, array of features) pairs.
   * @param numIterations Number of iterations of gradient descent to run.
   * @param stepSize Step size to be used for each iteration of gradient descent.
   * @param miniBatchFraction Fraction of data to be used per iteration.
   * @param initialWeights Initial set of weights to be used. Array should be equal in size to
   *        the number of features in the data.
   */
  def train(
    input: RDD[LabeledPoint],
    numIterations: Int,
    stepSize: Double,
    miniBatchFraction: Double,
    initialWeights: Array[Double]): LinearRegressionModel =
    {
      new LinearRegressionWithSGD(stepSize, numIterations, miniBatchFraction).run(input,
        initialWeights)
    }

  /**
   * Train a LinearRegression model given an RDD of (label, features) pairs. We run a fixed number
   * of iterations of gradient descent using the specified step size. Each iteration uses
   * `miniBatchFraction` fraction of the data to calculate the gradient.
   *
   * @param input RDD of (label, array of features) pairs.
   * @param numIterations Number of iterations of gradient descent to run.
   * @param stepSize Step size to be used for each iteration of gradient descent.
   * @param miniBatchFraction Fraction of data to be used per iteration.
   */
  def train(
    input: RDD[LabeledPoint],
    numIterations: Int,
    stepSize: Double,
    miniBatchFraction: Double): LinearRegressionModel =
    {
      new LinearRegressionWithSGD(stepSize, numIterations, miniBatchFraction).run(input)
    }

  /**
   * Train a LinearRegression model given an RDD of (label, features) pairs. We run a fixed number
   * of iterations of gradient descent using the specified step size. We use the entire data set to
   * update the gradient in each iteration.
   *
   * @param input RDD of (label, array of features) pairs.
   * @param stepSize Step size to be used for each iteration of Gradient Descent.
   * @param numIterations Number of iterations of gradient descent to run.
   * @return a LinearRegressionModel which has the weights and offset from training.
   */
  def train(
    input: RDD[LabeledPoint],
    numIterations: Int,
    stepSize: Double): LinearRegressionModel =
    {
      train(input, numIterations, stepSize, 1.0)
    }

  /**
   * Train a LinearRegression model given an RDD of (label, features) pairs. We run a fixed number
   * of iterations of gradient descent using a step size of 1.0. We use the entire data set to
   * update the gradient in each iteration.
   *
   * @param input RDD of (label, array of features) pairs.
   * @param numIterations Number of iterations of gradient descent to run.
   * @return a LinearRegressionModel which has the weights and offset from training.
   */
  def train(
    input: RDD[LabeledPoint],
    numIterations: Int): LinearRegressionModel =
    {
      train(input, numIterations, 1.0, 1.0)
    }

  def main(args: Array[String]) {
    if (args.length != 5) {
      println("Usage: LinearRegression <master> <input_dir> <step_size> <niters>")
      System.exit(1)
    }
    val sc = new SparkContext(args(0), "LinearRegression")
    val data = MLUtils.loadLabeledData(sc, args(1))
    val model = LinearRegressionWithSGD.train(data, args(3).toInt, args(2).toDouble)
    println("Weights: " + model.weights.mkString("[", ", ", "]"))
    println("Intercept: " + model.intercept)

    sc.stop()
  }
}
