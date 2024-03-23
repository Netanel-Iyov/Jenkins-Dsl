job('Seed All') {
  scm {
    git ('https://github.com/Netanel-Iyov/Jenkins-Dsl.git')
  }
  steps {
    dsl {
      external('pipelines/DSL**.groovy')  
      // default behavior
      // removeAction('IGNORE')      
      removeAction('DELETE')
    }
  }
}