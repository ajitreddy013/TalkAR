const chai = require("chai");
const chaiHttp = require("chai-http");
const app = require("../src/index");
const { expect } = chai;

chai.use(chaiHttp);

describe("Conversational Context API", () => {
  describe("POST /api/v1/ai-pipeline/conversational_query", () => {
    it("should process a conversational query successfully", (done) => {
      const requestBody = {
        query: "Hello, what is this?",
        imageId: "test-image-id",
        context: {
          userId: "test-user",
          sessionId: "test-session",
        },
      };

      chai
        .request(app)
        .post("/api/v1/ai-pipeline/conversational_query")
        .send(requestBody)
        .end((err, res) => {
          expect(res).to.have.status(200);
          expect(res.body).to.have.property("success", true);
          expect(res.body).to.have.property("response");
          expect(res.body.response).to.be.a("string");
          done();
        });
    });

    it("should return error when query is missing", (done) => {
      const requestBody = {
        imageId: "test-image-id",
      };

      chai
        .request(app)
        .post("/api/v1/ai-pipeline/conversational_query")
        .send(requestBody)
        .end((err, res) => {
          expect(res).to.have.status(400);
          expect(res.body).to.have.property("error");
          done();
        });
    });

    it("should process query without imageId", (done) => {
      const requestBody = {
        query: "Hello, how are you?",
      };

      chai
        .request(app)
        .post("/api/v1/ai-pipeline/conversational_query")
        .send(requestBody)
        .end((err, res) => {
          expect(res).to.have.status(200);
          expect(res.body).to.have.property("success", true);
          expect(res.body).to.have.property("response");
          done();
        });
    });

    it("should handle different types of queries", (done) => {
      const testQueries = [
        "Hello",
        "What is this?",
        "How does this work?",
        "Thank you",
        "Tell me more about this",
      ];

      let completedTests = 0;

      testQueries.forEach((query) => {
        const requestBody = {
          query: query,
          imageId: "test-image-id",
        };

        chai
          .request(app)
          .post("/api/v1/ai-pipeline/conversational_query")
          .send(requestBody)
          .end((err, res) => {
            expect(res).to.have.status(200);
            expect(res.body).to.have.property("success", true);
            expect(res.body).to.have.property("response");
            expect(res.body.response).to.be.a("string");

            completedTests++;
            if (completedTests === testQueries.length) {
              done();
            }
          });
      });
    });
  });
});
