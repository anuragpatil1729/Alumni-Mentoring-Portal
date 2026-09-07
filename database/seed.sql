-- Seed data for alumni_mentoring_portal

USE alumni_mentoring_portal;

-- Seed Users
INSERT INTO users (id, full_name, email, mobile_number, password_hash, role) VALUES
(101, 'Anurag Patil', 'anurag.patil@microsoft.com', '9876543210', 'scrypt$hash$demo1', 'alumni'),
(102, 'Vishwesh Bhilare', 'bhilarevishwesh@microsoft.com', '9876543211', 'scrypt$hash$demo2', 'alumni'),
(103, 'Rohan Mali', 'rohanmali@google.com', '9876543212', 'scrypt$hash$demo3', 'alumni'),
(104, 'Priya Sharma', 'priya.sharma@amazon.com', '9876543213', 'scrypt$hash$demo4', 'alumni'),
(105, 'Aditya Kulkarni', 'aditya.kulkarni@nvidia.com', '9876543214', 'scrypt$hash$demo5', 'alumni'),
(106, 'Sneha Deshmukh', 'sneha.d@goldmansachs.com', '9876543215', 'scrypt$hash$demo6', 'alumni'),
(107, 'Tanvi Joshi', 'tanvi.j@deloitte.com', '9876543216', 'scrypt$hash$demo7', 'alumni'),
(108, 'Kunal Verma', 'kunal.verma@apple.com', '9876543217', 'scrypt$hash$demo8', 'alumni'),
(109, 'Aarav Mehta', 'aarav.mehta@meta.com', '9876543218', 'scrypt$hash$demo9', 'alumni'),
(110, 'Neha Nair', 'neha.nair@mckinsey.com', '9876543219', 'scrypt$hash$demo10', 'alumni')
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

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
(201, 'Rahul Sharma', 'rahul.student@college.edu', '9876500001', 'plain$Student@123', 'student'),
(202, 'Ananya Patel', 'ananya.p@college.edu', '9876500002', 'plain$Student@123', 'student')
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

INSERT INTO students (user_id, student_id, department, graduation_year) VALUES
(201, 'STU-2024-001', 'Computer Engineering', 2026),
(202, 'STU-2024-002', 'Information Technology', 2025)
ON DUPLICATE KEY UPDATE student_id = VALUES(student_id);
