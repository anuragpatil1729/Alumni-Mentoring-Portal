-- Seed data for alumni_mentoring_portal

USE alumni_mentoring_portal;

-- Seed Users
INSERT INTO users (id, full_name, email, mobile_number, password_hash, role) VALUES
(101, 'Anurag Patil', 'anurag.patil@microsoft.com', '9876543210', 'sha256$61cc66b038a77f94057147f0a6f2ddd0$e47c7927149b94b8c3db632ecc86128e3f4870c16e17bff8fbb099cc6caf2b47', 'alumni'),
(102, 'Vishwesh Bhilare', 'bhilarevishwesh@microsoft.com', '9876543211', 'sha256$708b1b0f5e4ca50527b2634561e7e41e$167636d95b98f10f1c98e2533c89ceac6fee643b08e4574a55b369910d9346c4', 'alumni'),
(103, 'Rohan Mali', 'rohanmali@google.com', '9876543212', 'sha256$5ca36a4503d3cd36a08ef2977030ba32$b0acda76eec20d61b87106a282df2563fe9d4a8121f8247c89c90324ec19f8c6', 'alumni'),
(104, 'Priya Sharma', 'priya.sharma@amazon.com', '9876543213', 'sha256$586fb5b81483c7ee0ad937ef83473831$e5bc046bb1ca552c8296c7f6db22110b70ee9ee48b33162b6ef95739296e6be5', 'alumni'),
(105, 'Aditya Kulkarni', 'aditya.kulkarni@nvidia.com', '9876543214', 'sha256$324633aff761d4846765f3ecc670d4f6$f6bef91dd2e011ee2b596c5d21a210a3743813c5d4e5c07ed6af0773dfea9ce5', 'alumni'),
(106, 'Sneha Deshmukh', 'sneha.d@goldmansachs.com', '9876543215', 'sha256$81898e984259b7e7c11daaf1f9b721d3$eb19a20dddcf7caa40c9ca43696674cfab2d5cda09e5be8eeda3b0b7d37c2549', 'alumni'),
(107, 'Tanvi Joshi', 'tanvi.j@deloitte.com', '9876543216', 'sha256$91872292192f29dc015d14d7e61fda16$0b82b876db7a011497ff412f20b1469122796cfe559f042a3876f0009bdbc01a', 'alumni'),
(108, 'Kunal Verma', 'kunal.verma@apple.com', '9876543217', 'sha256$9e1fe8a352c1285d52251127b7ea8dc9$f496ef1298ab223f7dced53a4b62e4206d692049967fbd8074512d10a91a2d4d', 'alumni'),
(109, 'Aarav Mehta', 'aarav.mehta@meta.com', '9876543218', 'sha256$1d1afff546350ef0d67afedadb277595$f1cee8901e14e47e139e80dd3619dd7c609e993c1d1d765db9d42cb375005b7e', 'alumni'),
(110, 'Neha Nair', 'neha.nair@mckinsey.com', '9876543219', 'sha256$b49164c09b0addf4526f60c4a746eacd$af728aa1cc225f532b56bd11333ab8f939303486d64636a76bcde25f4a7a1bad', 'alumni')
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), password_hash = VALUES(password_hash);

-- Seed Alumni Profiles
INSERT INTO alumni (user_id, department, graduation_year, company, designation, linkedin_profile, experience_years, industry, skills, bio, max_mentees) VALUES
(101, 'Computer Engineering', 2022, 'Microsoft', 'Senior AI Engineer', 'https://linkedin.com/in/anurag-patil', 4, 'Information Technology', 'Java, Python, C++, Azure, Machine Learning, System Design', 'AI Engineer building distributed intelligence systems and LLM inference pipelines.', 5),
(102, 'Computer Engineering', 2022, 'Microsoft', 'Cloud Solutions Architect', 'https://linkedin.com/in/vishwesh-bhilare', 4, 'Information Technology', 'Java, Python, C, C++, Cloud Architecture, Kubernetes', 'Passionate about cloud-native infrastructure, distributed systems, and scalable microservices architectures.', 4),
(103, 'Computer Engineering', 2022, 'Google', 'Software Engineer II', 'https://linkedin.com/in/rohan-mali', 3, 'Information Technology', 'AI, Networking, OS, DBMS, Go, Kubernetes', 'Site Reliability & Core Systems engineer at Google. Mentoring students on operating systems, algorithms, and tech interview prep.', 3),
(104, 'Information Technology', 2021, 'Amazon', 'Software Development Engineer', 'https://linkedin.com/in/priya-sharma', 5, 'Information Technology', 'Java, Spring Boot, AWS, Distributed Systems, Microservices', 'Backend developer working on large-scale e-commerce transaction systems.', 5),
(105, 'Electronics & Telecommunication', 2020, 'Nvidia', 'Deep Learning Systems Engineer', 'https://linkedin.com/in/aditya-nvidia', 6, 'Information Technology', 'CUDA, C++, Python, PyTorch, TensorRT, GPU Computing', 'Specializing in accelerated computing, GPU kernel optimization, and high-performance deep learning inference.', 3),
(106, 'Computer Engineering', 2023, 'Goldman Sachs', 'Quantitative Strategist', 'https://linkedin.com/in/sneha-deshmukh', 2, 'Finance', 'Python, SQL, R, Financial Modeling, Algorithms, Statistics', 'Quantitative analysis and low-latency financial systems engineering.', 4),
(107, 'Information Technology', 2021, 'Deloitte', 'Cybersecurity Consultant', 'https://linkedin.com/in/tanvi-joshi', 4, 'Consulting', 'Cybersecurity, Penetration Testing, Risk Management, Network Security', 'Consultant helping Fortune 500 companies protect enterprise infrastructure and cloud posture.', 4),
(108, 'Computer Engineering', 2019, 'Apple', 'Senior iOS Frameworks Engineer', 'https://linkedin.com/in/kunal-verma', 7, 'Information Technology', 'Swift, Objective-C, SwiftUI, CoreData, Mobile Architecture', 'Working on native mobile frameworks and developer tools.', 6),
(109, 'Computer Engineering', 2020, 'Meta', 'Product Software Engineer', 'https://linkedin.com/in/aarav-mehta', 5, 'Information Technology', 'React, TypeScript, GraphQL, Relay, Node.js, Web Performance', 'Frontend infrastructure and web performance engineer.', 4),
(110, 'Mechanical Engineering', 2018, 'McKinsey & Company', 'Digital Transformation Consultant', 'https://linkedin.com/in/neha-nair', 7, 'Consulting', 'Strategy, Product Management, Analytics, Agile Transformation', 'Guiding engineering students looking to transition into strategy consulting.', 5)
ON DUPLICATE KEY UPDATE company = VALUES(company);

-- Seed Students
INSERT INTO users (id, full_name, email, mobile_number, password_hash, role) VALUES
(201, 'Rahul Sharma', 'rahul.student@college.edu', '9876500001', 'sha256$6059a8bf10e80b72028ab18804f47191$fd1cf7c5833ee530098a3c8d2a288098bb3524ecbdd8c6909ffa1c0b64421675', 'student'),
(202, 'Ananya Patel', 'ananya.p@college.edu', '9876500002', 'sha256$cf8eda7f3ad3b697dac38ba63070e609$0ddc4a59ec097c07e8f3057cba208eba0c9d76010528bda55f29d97b5bf31cc6', 'student')
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), password_hash = VALUES(password_hash);

INSERT INTO students (user_id, student_id, department, graduation_year) VALUES
(201, 'STU-2024-001', 'Computer Engineering', 2026),
(202, 'STU-2024-002', 'Information Technology', 2025)
ON DUPLICATE KEY UPDATE student_id = VALUES(student_id);
