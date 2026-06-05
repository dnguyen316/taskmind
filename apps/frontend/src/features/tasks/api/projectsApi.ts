import { listProjects as listCanonicalProjects } from '../../projects/api/projectsApi'

export async function listProjects() {
  return listCanonicalProjects({ includeArchived: false })
}
