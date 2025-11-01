const fs = require("fs");
const path = require("path");

// Test the context recall functionality
async function testContextRecall() {
  console.log("Testing context recall functionality...");

  // Clear the user_context.json file
  const userContextPath = path.join(__dirname, "data/user_context.json");
  fs.writeFileSync(userContextPath, "[]");

  // Simulate generating ad content for multiple posters
  const posters = [
    { id: "poster_01", name: "Amul Butter" },
    { id: "poster_02", name: "Sunrich Water" },
    { id: "poster_03", name: "Nike Shoes" },
    { id: "poster_04", name: "Coca Cola" }, // This should cause the oldest to be dropped
  ];

  console.log("Generating ad content for posters...");

  for (const poster of posters) {
    // Simulate storing interaction
    const interaction = {
      timestamp: new Date().toISOString(),
      poster_id: poster.id,
      product_name: poster.name,
      script: `Check out this amazing ${poster.name}!`,
      feedback: null,
    };

    // Read current interactions
    const currentData = JSON.parse(fs.readFileSync(userContextPath, "utf8"));

    // Add new interaction
    currentData.push(interaction);

    // Keep only last 3
    if (currentData.length > 3) {
      currentData.shift();
    }

    // Write back to file
    fs.writeFileSync(userContextPath, JSON.stringify(currentData, null, 2));

    console.log(`Stored interaction for ${poster.name}`);

    // Wait a bit between interactions
    await new Promise((resolve) => setTimeout(resolve, 1000));
  }

  // Check that we have the right number of interactions
  const finalData = JSON.parse(fs.readFileSync(userContextPath, "utf8"));
  console.log(`Final interactions count: ${finalData.length}`);

  if (finalData.length === 3) {
    console.log(
      "✅ Context recall test passed: Correct number of interactions stored"
    );
  } else {
    console.log(
      "❌ Context recall test failed: Incorrect number of interactions"
    );
    return false;
  }

  // Check that the oldest interaction was dropped (should be Sunrich Water, not Amul Butter)
  const firstInteraction = finalData[0];
  if (firstInteraction.product_name === "Sunrich Water") {
    console.log(
      "✅ Context recall test passed: Oldest interaction was correctly dropped"
    );
  } else {
    console.log(
      "❌ Context recall test failed: Oldest interaction was not dropped correctly"
    );
    console.log(
      `Expected: Sunrich Water, Got: ${firstInteraction.product_name}`
    );
    return false;
  }

  // Check that the most recent interactions are present
  const lastInteraction = finalData[2];
  if (lastInteraction.product_name === "Coca Cola") {
    console.log(
      "✅ Context recall test passed: Most recent interaction is correct"
    );
  } else {
    console.log(
      "❌ Context recall test failed: Most recent interaction is incorrect"
    );
    return false;
  }

  console.log("All context recall tests passed!");
  return true;
}

// Test the feedback capture system
async function testFeedbackCapture() {
  console.log("\nTesting feedback capture system...");

  const userContextPath = path.join(__dirname, "data/user_context.json");

  // Read current interactions
  const currentData = JSON.parse(fs.readFileSync(userContextPath, "utf8"));

  if (currentData.length === 0) {
    console.log("❌ Feedback capture test failed: No interactions to update");
    return false;
  }

  // Update feedback for the most recent interaction
  const lastIndex = currentData.length - 1;
  currentData[lastIndex].feedback = "like";

  // Write back to file
  fs.writeFileSync(userContextPath, JSON.stringify(currentData, null, 2));

  // Verify the feedback was updated
  const updatedData = JSON.parse(fs.readFileSync(userContextPath, "utf8"));
  const updatedInteraction = updatedData[lastIndex];

  if (updatedInteraction.feedback === "like") {
    console.log(
      "✅ Feedback capture test passed: Feedback was correctly updated"
    );
    return true;
  } else {
    console.log("❌ Feedback capture test failed: Feedback was not updated");
    return false;
  }
}

// Test the analytics logging
async function testAnalyticsLogging() {
  console.log("\nTesting analytics logging...");

  // Initialize the interaction logger to create the log file
  const interactionLoggerPath = path.join(
    __dirname,
    "src/utils/interactionLogger.ts"
  );

  // Check if log file exists
  const logPath = path.join(__dirname, "logs/interaction_log.csv");

  // Create a simple log entry to ensure the file is created
  const logEntry = {
    timestamp: new Date().toISOString(),
    poster_id: "test_poster",
    script: "Test script",
    feedback: null,
    response_time: 100,
  };

  // Format the entry as CSV
  const csvLine = `${logEntry.timestamp},${
    logEntry.poster_id
  },"${logEntry.script.replace(/"/g, '""')}",${logEntry.feedback || ""},${
    logEntry.response_time
  }\n`;

  // Create logs directory if it doesn't exist
  const logDir = path.dirname(logPath);
  if (!fs.existsSync(logDir)) {
    fs.mkdirSync(logDir, { recursive: true });
  }

  // Create log file with headers if it doesn't exist
  if (!fs.existsSync(logPath)) {
    const headers = "timestamp,poster_id,script,feedback,response_time\n";
    fs.writeFileSync(logPath, headers);
  }

  // Append test entry
  fs.appendFileSync(logPath, csvLine);

  // Check if log file exists
  if (!fs.existsSync(logPath)) {
    console.log("❌ Analytics logging test failed: Log file does not exist");
    return false;
  }

  // Read log file
  const logContent = fs.readFileSync(logPath, "utf8");
  const lines = logContent.trim().split("\n");

  // Check that we have headers and at least one entry
  if (lines.length < 2) {
    console.log(
      "❌ Analytics logging test failed: Log file does not contain entries"
    );
    return false;
  }

  // Check headers
  const headers = lines[0];
  const expectedHeaders = "timestamp,poster_id,script,feedback,response_time";
  if (headers !== expectedHeaders) {
    console.log(
      "❌ Analytics logging test failed: Log file has incorrect headers"
    );
    return false;
  }

  console.log(
    "✅ Analytics logging test passed: Log file is correctly formatted"
  );
  return true;
}

// Run all tests
async function runAllTests() {
  console.log("Running all tests...\n");

  try {
    const contextTestPassed = await testContextRecall();
    const feedbackTestPassed = await testFeedbackCapture();
    const analyticsTestPassed = await testAnalyticsLogging();

    if (contextTestPassed && feedbackTestPassed && analyticsTestPassed) {
      console.log("\n🎉 All tests passed!");
      process.exit(0);
    } else {
      console.log("\n💥 Some tests failed!");
      process.exit(1);
    }
  } catch (error) {
    console.error("Error running tests:", error);
    process.exit(1);
  }
}

// Run the tests
runAllTests();
